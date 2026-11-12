package co.nalu.negocio.catalogo.dto;

import am.ik.yavi.core.Validator;
import co.nalu.negocio.comun.EsquemaValidacion;
import java.math.BigDecimal;

public record MaterialRequest(String tipo, String ley, boolean requiereCertificado, BigDecimal factorPrecio) {

    public MaterialRequest {
        tipo = tipo == null ? null : tipo.strip();
        ley = ley == null || ley.isBlank() ? null : ley.strip();
    }

    public static final Validator<MaterialRequest> ESQUEMA = EsquemaValidacion.para(MaterialRequest.class)
            ._string(MaterialRequest::tipo, "tipo", c -> c.notBlank().lessThanOrEqual(30))
            ._string(MaterialRequest::ley, "ley", c -> c.lessThanOrEqual(20))
            ._bigDecimal(MaterialRequest::factorPrecio, "factorPrecio", c -> c.notNull().greaterThan(BigDecimal.ZERO)
                    .lessThan(new BigDecimal("1000")))
            .constraintOnTarget(m -> m.factorPrecio() == null || m.factorPrecio().stripTrailingZeros().scale() <= 3,
                    "factorPrecio", "decimales.maximo", "El factor admite como máximo 3 decimales")
            .build();
}
