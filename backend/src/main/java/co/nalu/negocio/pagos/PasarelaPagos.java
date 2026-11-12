package co.nalu.negocio.pagos;

import java.util.Map;

/**
 * Puerto hacia la pasarela de pagos (plan §4.2 regla 4). La implementa {@code integracion.wompi};
 * cambiar de pasarela no toca la lógica de negocio.
 */
public interface PasarelaPagos {

    /** Datos para que el navegador del cliente pague en la pasarela. */
    record DatosPago(String urlPago, String referencia, long montoCentavos, String moneda) {}

    /** Transacción informada por la pasarela, ya con la firma verificada. */
    record EventoTransaccion(String tipo, String idTransaccion, String referencia, String estado, long montoCentavos,
                             String metodo, Map<String, Object> carga) {}

    DatosPago prepararPago(String referencia, long montoCentavos, String correoCliente, String urlRetorno);

    /** @throws FirmaInvalidaException si la firma del evento no es válida */
    EventoTransaccion verificarEvento(String cuerpo);
}
