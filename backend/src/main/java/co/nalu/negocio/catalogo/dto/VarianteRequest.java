package co.nalu.negocio.catalogo.dto;

import am.ik.yavi.core.Validator;
import co.nalu.negocio.comun.EsquemaValidacion;
import java.math.BigDecimal;

public record VarianteRequest(String talla, BigDecimal pesoGramos, BigDecimal precioAdicional, boolean activa) {

    public VarianteRequest {
        talla = talla == null || talla.isBlank() ? null : talla.strip();
    }

    public static final Validator<VarianteRequest> ESQUEMA = EsquemaValidacion.para(VarianteRequest.class)
            ._string(VarianteRequest::talla, "talla", c -> c.lessThanOrEqual(20))
            ._bigDecimal(VarianteRequest::pesoGramos, "pesoGramos", c -> c.greaterThan(BigDecimal.ZERO)
                    .lessThan(new BigDecimal("1000000")))
            ._bigDecimal(VarianteRequest::precioAdicional, "precioAdicional", c -> c.notNull()
                    .greaterThanOrEqual(BigDecimal.ZERO).lessThan(new BigDecimal("10000000000")))
            .build();
}
