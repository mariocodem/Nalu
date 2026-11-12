package co.nalu.negocio.pagos;

/** El evento de la pasarela no trae una firma válida: se rechaza con 401 sin tocar nada (plan §6.4). */
public class FirmaInvalidaException extends RuntimeException {

    public FirmaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
