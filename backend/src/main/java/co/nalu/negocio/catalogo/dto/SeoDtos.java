package co.nalu.negocio.catalogo.dto;

import am.ik.yavi.core.Validator;
import co.nalu.negocio.comun.EsquemaValidacion;
import java.util.Map;

public final class SeoDtos {

    private SeoDtos() {}

    /** RF05. Los campos vacíos se rellenan al publicar con datos del producto o la categoría. */
    public record SeoRequest(String titulo, String metaDescripcion, String palabrasClave, String urlCanonica,
                             Map<String, Object> datosEstructurados) {

        public SeoRequest {
            titulo = vacio(titulo);
            metaDescripcion = vacio(metaDescripcion);
            palabrasClave = vacio(palabrasClave);
            urlCanonica = vacio(urlCanonica);
        }

        private static String vacio(String valor) {
            return valor == null || valor.isBlank() ? null : valor.strip();
        }

        public static final Validator<SeoRequest> ESQUEMA = EsquemaValidacion.para(SeoRequest.class)
                ._string(SeoRequest::titulo, "titulo", c -> c.lessThanOrEqual(160))
                ._string(SeoRequest::metaDescripcion, "metaDescripcion", c -> c.lessThanOrEqual(320))
                ._string(SeoRequest::palabrasClave, "palabrasClave", c -> c.lessThanOrEqual(255))
                ._string(SeoRequest::urlCanonica, "urlCanonica", c -> c.lessThanOrEqual(255).url()
                        .message("La URL canónica debe ser una URL completa (https://…)"))
                .constraintOnTarget(s -> s.datosEstructurados() == null || !s.datosEstructurados().containsKey("@type"),
                        "datosEstructurados", "seo.tipo", "El tipo de Schema.org lo fija Nalu (Product); quita «@type»")
                .build();
    }

    public record SeoAdmin(String titulo, String metaDescripcion, String palabrasClave, String urlCanonica,
                           Map<String, Object> datosEstructurados) {}
}
