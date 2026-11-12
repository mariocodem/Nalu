package co.nalu.negocio.comun;

import am.ik.yavi.builder.ValidatorBuilder;

/**
 * Punto de partida de todos los esquemas YAVI de Nalu (plan de desarrollo §4.6).
 *
 * <p>Usar siempre este método en lugar de {@code ValidatorBuilder.of()} para que los mensajes
 * salgan en español:
 *
 * <pre>{@code
 * public static final Validator<RegistroClienteRequest> ESQUEMA =
 *     EsquemaValidacion.para(RegistroClienteRequest.class)
 *         ._string(RegistroClienteRequest::correo, "correo", c -> c.notBlank().email())
 *         .build();
 * }</pre>
 */
public final class EsquemaValidacion {

    private EsquemaValidacion() {}

    public static <T> ValidatorBuilder<T> para(Class<T> tipo) {
        return ValidatorBuilder.of(tipo).messageFormatter(FormateadorMensajesValidacion.instancia());
    }
}
