package co.nalu.negocio.catalogo.dto;

import am.ik.yavi.core.Validator;
import co.nalu.negocio.comun.EsquemaValidacion;
import java.math.BigDecimal;

public record VarianteNuevaRequest(String sku, String talla, BigDecimal pesoGramos, BigDecimal precioAdicional,
                                   Integer existenciaInicial) {

    public VarianteNuevaRequest {
        sku = sku == null ? null : sku.strip().toUpperCase(java.util.Locale.ROOT);
        talla = talla == null || talla.isBlank() ? null : talla.strip();
    }

    public static final Validator<VarianteNuevaRequest> ESQUEMA = EsquemaValidacion.para(VarianteNuevaRequest.class)
            ._string(VarianteNuevaRequest::sku, "sku", c -> c.notBlank().pattern(ProductoRequest.PATRON_SKU)
                    .message("El SKU debe tener de 3 a 40 letras mayúsculas, números o guiones"))
            ._string(VarianteNuevaRequest::talla, "talla", c -> c.lessThanOrEqual(20))
            ._bigDecimal(VarianteNuevaRequest::pesoGramos, "pesoGramos", c -> c.greaterThan(BigDecimal.ZERO)
                    .lessThan(new BigDecimal("1000000")))
            ._bigDecimal(VarianteNuevaRequest::precioAdicional, "precioAdicional", c -> c.notNull()
                    .greaterThanOrEqual(BigDecimal.ZERO).lessThan(new BigDecimal("10000000000")))
            ._integer(VarianteNuevaRequest::existenciaInicial, "existenciaInicial", c -> c.notNull()
                    .greaterThanOrEqual(0).lessThanOrEqual(100000))
            .build();
}
