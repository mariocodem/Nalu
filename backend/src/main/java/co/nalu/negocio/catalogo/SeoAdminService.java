package co.nalu.negocio.catalogo;

import co.nalu.datos.entidad.MetadatoSeo;
import co.nalu.datos.repositorio.CategoriaRepository;
import co.nalu.datos.repositorio.MetadatoSeoRepository;
import co.nalu.datos.repositorio.ProductoRepository;
import co.nalu.negocio.catalogo.dto.SeoDtos.SeoAdmin;
import co.nalu.negocio.catalogo.dto.SeoDtos.SeoRequest;
import co.nalu.negocio.comun.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF05 / CU-15: metadatos SEO editables de productos y categorías. */
@Service
public class SeoAdminService {

    private static final SeoAdmin VACIO = new SeoAdmin(null, null, null, null, null);

    private final MetadatoSeoRepository metadatos;
    private final ProductoRepository productos;
    private final CategoriaRepository categorias;

    public SeoAdminService(MetadatoSeoRepository metadatos, ProductoRepository productos, CategoriaRepository categorias) {
        this.metadatos = metadatos;
        this.productos = productos;
        this.categorias = categorias;
    }

    @Transactional(readOnly = true)
    public SeoAdmin deProducto(long idProducto) {
        productos.findById(idProducto).orElseThrow(() -> new RecursoNoEncontradoException("El producto no existe"));
        return metadatos.findByProductoId(idProducto).map(SeoAdminService::dto).orElse(VACIO);
    }

    @Transactional
    public SeoAdmin guardarDeProducto(long idProducto, SeoRequest s) {
        var meta = metadatos.findByProductoId(idProducto).orElseGet(() -> metadatos.save(MetadatoSeo.deProducto(
                productos.findById(idProducto).orElseThrow(() -> new RecursoNoEncontradoException("El producto no existe")))));
        meta.actualizar(s.titulo(), s.metaDescripcion(), s.palabrasClave(), s.urlCanonica(), s.datosEstructurados());
        return dto(meta);
    }

    @Transactional(readOnly = true)
    public SeoAdmin deCategoria(long idCategoria) {
        categorias.findById(idCategoria).orElseThrow(() -> new RecursoNoEncontradoException("La categoría no existe"));
        return metadatos.findByCategoriaId(idCategoria).map(SeoAdminService::dto).orElse(VACIO);
    }

    @Transactional
    public SeoAdmin guardarDeCategoria(long idCategoria, SeoRequest s) {
        var meta = metadatos.findByCategoriaId(idCategoria).orElseGet(() -> metadatos.save(MetadatoSeo.deCategoria(
                categorias.findById(idCategoria).orElseThrow(() -> new RecursoNoEncontradoException("La categoría no existe")))));
        meta.actualizar(s.titulo(), s.metaDescripcion(), s.palabrasClave(), s.urlCanonica(), s.datosEstructurados());
        return dto(meta);
    }

    private static SeoAdmin dto(MetadatoSeo m) {
        return new SeoAdmin(m.getTitulo(), m.getMetaDescripcion(), m.getPalabrasClave(), m.getUrlCanonica(),
                m.getDatosEstructurados());
    }
}
