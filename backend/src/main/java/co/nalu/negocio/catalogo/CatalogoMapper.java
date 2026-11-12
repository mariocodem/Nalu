package co.nalu.negocio.catalogo;

import co.nalu.datos.entidad.Categoria;
import co.nalu.datos.entidad.ImagenProducto;
import co.nalu.datos.entidad.Material;
import co.nalu.datos.entidad.Producto;
import co.nalu.datos.entidad.VarianteProducto;
import co.nalu.negocio.catalogo.dto.CategoriaAdminDto;
import co.nalu.negocio.catalogo.dto.MaterialDto;
import co.nalu.negocio.catalogo.dto.ProductoAdminDto;
import co.nalu.negocio.catalogo.dto.ProductoResumenDto;
import java.util.Comparator;

/** Entidad → DTO del catálogo. Se usa dentro de transacciones (las relaciones son LAZY). */
final class CatalogoMapper {

    private CatalogoMapper() {}

    static CategoriaAdminDto categoria(Categoria c) {
        var hijos = c.getHijos().stream()
                .sorted(Comparator.comparing(Categoria::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(CatalogoMapper::categoria)
                .toList();
        return new CategoriaAdminDto(c.getId(), c.getNombre(), c.getSlug(), c.getDescripcion(), c.isActiva(),
                c.getPadre() == null ? null : c.getPadre().getId(), hijos);
    }

    static MaterialDto material(Material m) {
        return new MaterialDto(m.getId(), m.getTipo(), m.getLey(), m.isRequiereCertificado(), m.getFactorPrecio());
    }

    static ProductoResumenDto resumen(Producto p) {
        int existencia = p.getVariantes().stream().mapToInt(VarianteProducto::getStockActual).sum();
        var principal = p.getImagenes().stream().filter(ImagenProducto::isPrincipal).findFirst()
                .map(ImagenProducto::getUrl).orElse(null);
        return new ProductoResumenDto(p.getId(), p.getSku(), p.getNombre(), p.getSlug(), p.getCategoria().getNombre(),
                p.getPrecioBase(), p.getEstado(), existencia, principal);
    }

    static ProductoAdminDto producto(Producto p) {
        var materiales = p.getMateriales().stream()
                .map(pm -> new ProductoAdminDto.MaterialProducto(pm.getMaterial().getId(), pm.getMaterial().getTipo(),
                        pm.getMaterial().getLey(), pm.getRol(), pm.getPesoGramos(), pm.getQuilates()))
                .toList();
        var variantes = p.getVariantes().stream()
                .sorted(Comparator.comparing(VarianteProducto::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(v -> new ProductoAdminDto.Variante(v.getId(), v.getSku(), v.getTalla(), v.getPesoGramos(),
                        v.getPrecioAdicional(), v.getStockActual(), v.getStockReservado(), v.isActiva()))
                .toList();
        var imagenes = p.getImagenes().stream()
                .sorted(Comparator.comparingInt(ImagenProducto::getOrden))
                .map(i -> new ProductoAdminDto.Imagen(i.getId(), i.getUrl(), i.getTextoAlternativo(), i.getOrden(),
                        i.isPrincipal()))
                .toList();
        return new ProductoAdminDto(p.getId(), p.getSku(), p.getNombre(), p.getSlug(), p.getDescripcion(),
                p.getCategoria().getId(), p.getPrecioBase(), p.getEstado(), p.getFechaCreacion(), materiales,
                variantes, imagenes);
    }
}
