package co.nalu.negocio.asistente.dto;

import co.nalu.negocio.catalogo.dto.CatalogoPublicoDtos.ImagenPublica;
import co.nalu.negocio.catalogo.dto.CatalogoPublicoDtos.VarianteFicha;
import co.nalu.negocio.comun.EsquemaValidacion;
import am.ik.yavi.core.Validator;
import java.math.BigDecimal;
import java.util.List;

/** Contrato del asistente de compras: docs/arquitectura/api-asistente.md. */
public final class AsistenteDtos {

    private AsistenteDtos() {}

    public static final int MAX_MENSAJE = 1000;
    public static final int MAX_HISTORIAL = 20;
    public static final int MAX_TEXTO_HISTORIAL = 4000;

    /** @param rol {@code cliente} o {@code asistente} */
    public record MensajeHistorial(String rol, String texto) {}

    /**
     * La conversación vive en el navegador: cada mensaje llega con el historial anterior.
     *
     * @param historial mensajes previos, del más antiguo al más reciente (opcional)
     * @param mensaje lo que el cliente acaba de escribir
     */
    public record SolicitudAsistente(List<MensajeHistorial> historial, String mensaje) {
        public static final Validator<SolicitudAsistente> ESQUEMA = EsquemaValidacion.para(SolicitudAsistente.class)
                ._string(SolicitudAsistente::mensaje, "mensaje", c -> c.notBlank().lessThanOrEqual(MAX_MENSAJE))
                ._collection(SolicitudAsistente::historial, "historial", c -> c.lessThanOrEqual(MAX_HISTORIAL))
                .forEachIfPresent(SolicitudAsistente::historial, "historial", b -> b
                        ._string(MensajeHistorial::rol, "rol", c -> c.notNull().oneOf(List.of("cliente", "asistente")))
                        ._string(MensajeHistorial::texto, "texto", c -> c.notBlank().lessThanOrEqual(MAX_TEXTO_HISTORIAL)))
                .build();
    }

    /** Pieza que el asistente muestra como tarjeta; todos los datos salen del catálogo, no del modelo. */
    public record PiezaSugerida(String slug, String nombre, ImagenPublica imagen, String material,
                                BigDecimal precioDesde, boolean agotado, List<VarianteFicha> variantes) {}

    public record RespuestaAsistente(String texto, List<PiezaSugerida> piezas) {}

    public record EstadoAsistente(boolean disponible) {}
}
