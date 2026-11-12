package co.nalu.negocio.asistente;

/** El asistente no está configurado o el proveedor del modelo no respondió. La API responde 503. */
public class AsistenteNoDisponibleException extends RuntimeException {

    public AsistenteNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
