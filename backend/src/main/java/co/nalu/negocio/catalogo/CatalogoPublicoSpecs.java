package co.nalu.negocio.catalogo;

import co.nalu.datos.entidad.Categoria_;
import co.nalu.datos.entidad.EstadoProducto;
import co.nalu.datos.entidad.Material_;
import co.nalu.datos.entidad.Producto;
import co.nalu.datos.entidad.ProductoMaterial;
import co.nalu.datos.entidad.ProductoMaterial_;
import co.nalu.datos.entidad.Producto_;
import co.nalu.datos.entidad.VarianteProducto;
import co.nalu.datos.entidad.VarianteProducto_;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros del catálogo público (RF06) con Criteria API: sin SQL nativo. Material y disponibilidad
 * usan subconsultas {@code EXISTS} para no duplicar filas ni romper la paginación.
 */
final class CatalogoPublicoSpecs {

    private CatalogoPublicoSpecs() {}

    static Specification<Producto> visible() {
        return (r, q, cb) -> cb.and(cb.equal(r.get(Producto_.estado), EstadoProducto.PUBLICADO),
                cb.isTrue(r.get(Producto_.categoria).get(Categoria_.activa)));
    }

    static Specification<Producto> enCategorias(Collection<Long> ids) {
        return (r, q, cb) -> ids == null ? null : r.get(Producto_.categoria).get(Categoria_.id).in(ids);
    }

    static Specification<Producto> conMaterial(Long idMaterial) {
        return (r, q, cb) -> {
            if (idMaterial == null) {
                return null;
            }
            var sub = q.subquery(Integer.class);
            var pm = sub.from(ProductoMaterial.class);
            sub.select(cb.literal(1)).where(cb.equal(pm.get(ProductoMaterial_.producto), r),
                    cb.equal(pm.get(ProductoMaterial_.material).get(Material_.id), idMaterial));
            return cb.exists(sub);
        };
    }

    static Specification<Producto> disponible(Boolean soloDisponibles) {
        return (r, q, cb) -> {
            if (!Boolean.TRUE.equals(soloDisponibles)) {
                return null;
            }
            var sub = q.subquery(Integer.class);
            var v = sub.from(VarianteProducto.class);
            sub.select(cb.literal(1)).where(cb.equal(v.get(VarianteProducto_.producto), r),
                    cb.isTrue(v.get(VarianteProducto_.activa)),
                    cb.gt(cb.diff(v.get(VarianteProducto_.stockActual), v.get(VarianteProducto_.stockReservado)), 0));
            return cb.exists(sub);
        };
    }

    static Specification<Producto> precioEntre(BigDecimal minimo, BigDecimal maximo) {
        return (r, q, cb) -> {
            if (minimo == null && maximo == null) {
                return null;
            }
            var precio = r.get(Producto_.precioDesde);
            if (minimo != null && maximo != null) {
                return cb.between(precio, minimo, maximo);
            }
            return minimo != null ? cb.greaterThanOrEqualTo(precio, minimo) : cb.lessThanOrEqualTo(precio, maximo);
        };
    }

    static Specification<Producto> texto(String texto) {
        return (r, q, cb) -> {
            if (texto == null || texto.isBlank()) {
                return null;
            }
            var patron = "%" + texto.strip().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%")
                    .replace("_", "\\_") + "%";
            return cb.or(cb.like(cb.lower(r.get(Producto_.nombre)), patron, '\\'),
                    cb.like(cb.lower(r.get(Producto_.descripcion).as(String.class)), patron, '\\'));
        };
    }
}
