package co.nalu.negocio.catalogo;

import co.nalu.datos.entidad.Categoria;
import co.nalu.datos.repositorio.CategoriaRepository;
import co.nalu.negocio.catalogo.dto.CategoriaAdminDto;
import co.nalu.negocio.catalogo.dto.CategoriaRequest;
import co.nalu.negocio.comun.ConflictoException;
import co.nalu.negocio.comun.RecursoNoEncontradoException;
import co.nalu.negocio.comun.Slugs;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF03: categorías jerárquicas. No se borran: se activan o desactivan. */
@Service
public class CategoriaService {

    private final CategoriaRepository categorias;

    public CategoriaService(CategoriaRepository categorias) {
        this.categorias = categorias;
    }

    @Transactional(readOnly = true)
    public List<CategoriaAdminDto> arbol() {
        return categorias.findAllByOrderByNombreAsc().stream()
                .filter(c -> c.getPadre() == null)
                .map(CatalogoMapper::categoria)
                .toList();
    }

    @Transactional
    public CategoriaAdminDto crear(CategoriaRequest solicitud) {
        var slug = Slugs.elegir(solicitud.slug(), solicitud.nombre());
        if (categorias.existsBySlug(slug)) {
            throw new ConflictoException("Ya existe una categoría con el slug " + slug);
        }
        var categoria = new Categoria(solicitud.nombre(), slug, solicitud.descripcion(), padre(solicitud.idPadre()));
        return CatalogoMapper.categoria(categorias.save(categoria));
    }

    @Transactional
    public CategoriaAdminDto actualizar(long id, CategoriaRequest solicitud) {
        var categoria = buscar(id);
        var slug = Slugs.elegir(solicitud.slug(), solicitud.nombre());
        if (categorias.existsBySlugAndIdNot(slug, id)) {
            throw new ConflictoException("Ya existe una categoría con el slug " + slug);
        }
        try {
            categoria.moverA(padre(solicitud.idPadre()));
        } catch (IllegalArgumentException e) {
            throw new ConflictoException(e.getMessage());
        }
        categoria.actualizar(solicitud.nombre(), slug, solicitud.descripcion());
        return CatalogoMapper.categoria(categoria);
    }

    @Transactional
    public CategoriaAdminDto cambiarEstado(long id, boolean activa) {
        var categoria = buscar(id);
        if (activa) {
            categoria.activar();
        } else {
            categoria.desactivar();
        }
        return CatalogoMapper.categoria(categoria);
    }

    Categoria buscar(long id) {
        return categorias.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La categoría no existe"));
    }

    private Categoria padre(Long idPadre) {
        return idPadre == null ? null : buscar(idPadre);
    }
}
