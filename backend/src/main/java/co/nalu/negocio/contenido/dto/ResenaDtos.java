package co.nalu.negocio.contenido.dto;

import am.ik.yavi.core.Validator;
import co.nalu.datos.entidad.EstadoResena;
import co.nalu.negocio.comun.EsquemaValidacion;
import java.time.OffsetDateTime;
import java.util.List;

public final class ResenaDtos {

    private ResenaDtos() {}

    public record ResenaRequest(Integer calificacion, String comentario) {
        public ResenaRequest {
            comentario = comentario == null || comentario.isBlank() ? null : comentario.strip();
        }

        public static final Validator<ResenaRequest> ESQUEMA = EsquemaValidacion.para(ResenaRequest.class)
                ._integer(ResenaRequest::calificacion, "calificacion", c -> c.notNull().greaterThanOrEqual(1).lessThanOrEqual(5))
                ._string(ResenaRequest::comentario, "comentario", c -> c.lessThanOrEqual(1000))
                .build();
    }

    /** Reseña publicada: solo el nombre y la inicial del apellido de quien la escribió. */
    public record ResenaPublica(String autor, int calificacion, String comentario, OffsetDateTime fecha) {}

    public record Resumen(Double promedio, int total, List<ResenaPublica> resenas) {}

    public record ResenaAdmin(Long id, String producto, String slug, String cliente, String correo, int calificacion,
                              String comentario, OffsetDateTime fecha, EstadoResena estado) {}

    public record ModerarRequest(EstadoResena estado) {}

    public record FiltroResenas(EstadoResena estado) {}
}
