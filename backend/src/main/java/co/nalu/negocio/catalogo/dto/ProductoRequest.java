package co.nalu.negocio.catalogo.dto;

import am.ik.yavi.core.Validator;
import co.nalu.negocio.comun.EsquemaValidacion;
import co.nalu.negocio.comun.Slugs;
import java.math.BigDecimal;

public record ProductoRequest(String sku, String nombre, String slug, String descripcion, Long idCategoria,
                              BigDecimal precioBase) {

    public static final String PATRON_SKU = "^[A-Z0-9-]{3,40}$";

    public ProductoRequest {
        sku = sku == null ? null : sku.strip().toUpperCase(java.util.Locale.ROOT);
        nombre = nombre == null ? null : nombre.strip();
        slug = slug == null || slug.isBlank() ? null : slug.strip();
        descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.strip();
    }

    public static final Validator<ProductoRequest> ESQUEMA = EsquemaValidacion.para(ProductoRequest.class)
            ._string(ProductoRequest::sku, "sku", c -> c.notBlank().pattern(PATRON_SKU)
                    .message("El SKU debe tener de 3 a 40 letras mayúsculas, números o guiones"))
            ._string(ProductoRequest::nombre, "nombre", c -> c.notBlank().lessThanOrEqual(120))
            ._string(ProductoRequest::slug, "slug", c -> c.lessThanOrEqual(140).pattern(Slugs.PATRON)
                    .message("El slug solo admite minúsculas, números y guiones"))
            ._string(ProductoRequest::descripcion, "descripcion", c -> c.lessThanOrEqual(5000))
            ._long(ProductoRequest::idCategoria, "idCategoria", c -> c.notNull())
            ._bigDecimal(ProductoRequest::precioBase, "precioBase", c -> c.notNull().greaterThan(BigDecimal.ZERO)
                    .lessThan(new BigDecimal("10000000000")))
            .constraintOnTarget(p -> p.precioBase() == null || p.precioBase().stripTrailingZeros().scale() <= 2,
                    "precioBase", "decimales.maximo", "El precio admite como máximo 2 decimales")
            .build();
}
