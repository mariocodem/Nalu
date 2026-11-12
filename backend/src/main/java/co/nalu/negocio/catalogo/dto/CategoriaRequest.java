package co.nalu.negocio.catalogo.dto;

import am.ik.yavi.core.Validator;
import co.nalu.negocio.comun.EsquemaValidacion;
import co.nalu.negocio.comun.Slugs;

public record CategoriaRequest(String nombre, String slug, String descripcion, Long idPadre) {

    public CategoriaRequest {
        nombre = nombre == null ? null : nombre.strip();
        slug = slug == null || slug.isBlank() ? null : slug.strip();
        descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.strip();
    }

    public static final Validator<CategoriaRequest> ESQUEMA = EsquemaValidacion.para(CategoriaRequest.class)
            ._string(CategoriaRequest::nombre, "nombre", c -> c.notBlank().lessThanOrEqual(80))
            ._string(CategoriaRequest::slug, "slug", c -> c.lessThanOrEqual(100).pattern(Slugs.PATRON)
                    .message("El slug solo admite minúsculas, números y guiones"))
            ._string(CategoriaRequest::descripcion, "descripcion", c -> c.lessThanOrEqual(2000))
            .build();
}
