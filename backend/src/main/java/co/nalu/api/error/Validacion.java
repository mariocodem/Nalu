package co.nalu.api.error;

import am.ik.yavi.core.Validator;
import co.nalu.negocio.comun.FormateadorMensajesValidacion;
import java.util.List;

/**
 * Valida la entrada de un controlador con el esquema YAVI del DTO antes de delegar en la capa de
 * negocio (plan de desarrollo §4.6).
 *
 * <pre>{@code
 * validar(req, RegistroClienteRequest.ESQUEMA);
 * }</pre>
 */
public final class Validacion {

    private Validacion() {}

    /**
     * @return el mismo DTO, para encadenar
     * @throws EntradaInvalidaException con todos los errores encontrados, no solo el primero
     */
    public static <T> T validar(T dto, Validator<T> esquema) {
        if (dto == null) {
            throw new EntradaInvalidaException(
                    List.of(new ErrorCampo("", "El cuerpo de la solicitud es obligatorio")));
        }
        esquema.validate(dto, FormateadorMensajesValidacion.ESPANOL_COLOMBIA)
                .throwIfInvalid(EntradaInvalidaException::new);
        return dto;
    }
}
