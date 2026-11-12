package co.nalu.negocio.comun;

/** Se superó el límite de intentos permitido para una operación. La API responde 429. */
public class DemasiadosIntentosException extends RuntimeException {

    public DemasiadosIntentosException(String mensaje) {
        super(mensaje);
    }
}
