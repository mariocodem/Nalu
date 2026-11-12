package co.nalu.negocio.catalogo.dto;

import am.ik.yavi.core.Validator;
import co.nalu.negocio.comun.EsquemaValidacion;

public record ImagenRequest(String textoAlternativo, Integer orden, boolean principal) {

    public ImagenRequest {
        textoAlternativo = textoAlternativo == null ? null : textoAlternativo.strip();
    }

    public static final Validator<ImagenRequest> ESQUEMA = EsquemaValidacion.para(ImagenRequest.class)
            ._string(ImagenRequest::textoAlternativo, "textoAlternativo", c -> c.notBlank().lessThanOrEqual(160))
            ._integer(ImagenRequest::orden, "orden", c -> c.notNull().greaterThanOrEqual(0).lessThanOrEqual(1000))
            .build();
}
