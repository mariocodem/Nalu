package co.nalu.api.error;

/** El header {@code Origin} no coincide con el frontend configurado (plan §8.5). Responde 403. */
public class OrigenNoPermitidoException extends RuntimeException {

    public OrigenNoPermitidoException() {
        super("Origen no permitido");
    }
}
