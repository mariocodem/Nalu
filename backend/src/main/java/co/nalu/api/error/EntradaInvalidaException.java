package co.nalu.api.error;

import am.ik.yavi.core.ConstraintViolations;
import java.util.List;

/** La solicitud no cumple su esquema YAVI. Se responde con 400 y la lista de errores por campo. */
public class EntradaInvalidaException extends RuntimeException {

    private final List<ErrorCampo> errores;

    public EntradaInvalidaException(List<ErrorCampo> errores) {
        super("Datos inválidos: " + errores.size() + " error(es)");
        this.errores = List.copyOf(errores);
    }

    public EntradaInvalidaException(ConstraintViolations violaciones) {
        this(violaciones.stream().map(v -> new ErrorCampo(v.name(), v.message())).toList());
    }

    public List<ErrorCampo> errores() {
        return errores;
    }
}
