package co.nalu.negocio.catalogo;

import co.nalu.datos.entidad.Material;
import co.nalu.datos.repositorio.MaterialRepository;
import co.nalu.datos.repositorio.ProductoRepository;
import co.nalu.negocio.catalogo.dto.MaterialDto;
import co.nalu.negocio.catalogo.dto.MaterialRequest;
import co.nalu.negocio.auditoria.AuditoriaService;
import co.nalu.negocio.comun.ConflictoException;
import co.nalu.negocio.comun.RecursoNoEncontradoException;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Materiales del catálogo (acero, plata, oro, esmeralda, cuarzo…) con su ley y factor de precio. */
@Service
public class MaterialService {

    private final MaterialRepository materiales;
    private final ProductoRepository productos;
    private final CalculadoraPrecio calculadora;
    private final AuditoriaService auditoria;

    public MaterialService(MaterialRepository materiales, ProductoRepository productos, CalculadoraPrecio calculadora,
                           AuditoriaService auditoria) {
        this.materiales = materiales;
        this.productos = productos;
        this.calculadora = calculadora;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<MaterialDto> listar() {
        return materiales.findAllByOrderByTipoAscLeyAsc().stream().map(CatalogoMapper::material).toList();
    }

    @Transactional
    public MaterialDto crear(MaterialRequest solicitud) {
        verificarQueNoExista(solicitud, null);
        var material = new Material(solicitud.tipo(), solicitud.ley(), solicitud.requiereCertificado(),
                solicitud.factorPrecio());
        return CatalogoMapper.material(materiales.save(material));
    }

    @Transactional
    public MaterialDto actualizar(long id, MaterialRequest solicitud) {
        var material = buscar(id);
        verificarQueNoExista(solicitud, material);
        if (material.getFactorPrecio().compareTo(solicitud.factorPrecio()) != 0) {
            auditoria.registrarCambio("CAMBIO_PRECIO", "materiales", id, "Factor de precio de " + material.getTipo() + " "
                    + material.getFactorPrecio().toPlainString() + " → " + solicitud.factorPrecio().toPlainString());
        }
        material.actualizar(solicitud.tipo(), solicitud.ley(), solicitud.requiereCertificado(), solicitud.factorPrecio());
        // El factor cambia el precio de venta de todas las piezas con este material (ADR 003).
        for (var producto : productos.findDistinctByMateriales_Material_Id(id)) {
            producto.actualizarPrecioDesde(calculadora.precioDesde(producto));
        }
        return CatalogoMapper.material(material);
    }

    Material buscar(long id) {
        return materiales.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("El material no existe"));
    }

    private void verificarQueNoExista(MaterialRequest solicitud, Material actual) {
        boolean mismo = actual != null && actual.getTipo().equalsIgnoreCase(solicitud.tipo())
                && Objects.equals(actual.getLey(), solicitud.ley());
        boolean existe = solicitud.ley() == null
                ? materiales.existsByTipoIgnoreCaseAndLeyIsNull(solicitud.tipo())
                : materiales.existsByTipoIgnoreCaseAndLey(solicitud.tipo(), solicitud.ley());
        if (existe && !mismo) {
            throw new ConflictoException("Ya existe el material " + solicitud.tipo()
                    + (solicitud.ley() == null ? "" : " " + solicitud.ley()));
        }
    }
}
