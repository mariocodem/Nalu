package co.nalu.negocio.catalogo;

import co.nalu.datos.entidad.Categoria_;
import co.nalu.datos.entidad.EstadoProducto;
import co.nalu.datos.entidad.Producto;
import co.nalu.datos.entidad.Producto_;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Filtros tipados de productos con Criteria API y el metamodelo estático (plan §5.4). */
final class ProductoSpecs {

    private ProductoSpecs() {}

    static Specification<Producto> textoEnNombreOSku(String texto) {
        return (r, q, cb) -> {
            if (texto == null || texto.isBlank()) {
                return null;
            }
            var patron = "%" + escapar(texto.strip().toLowerCase(Locale.ROOT)) + "%";
            return cb.or(cb.like(cb.lower(r.get(Producto_.nombre)), patron, '\\'),
                    cb.like(cb.lower(r.get(Producto_.sku)), patron, '\\'));
        };
    }

    static Specification<Producto> conEstado(EstadoProducto estado) {
        return (r, q, cb) -> estado == null ? null : cb.equal(r.get(Producto_.estado), estado);
    }

    static Specification<Producto> deCategoria(Long idCategoria) {
        return (r, q, cb) -> idCategoria == null ? null
                : cb.equal(r.get(Producto_.categoria).get(Categoria_.id), idCategoria);
    }

    /** Los comodines que escribe el usuario se buscan literalmente. */
    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
