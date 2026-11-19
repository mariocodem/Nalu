package co.nalu.negocio.pagos;

import java.util.Map;

/**
 * Puerto hacia la pasarela de pagos (plan §4.2 regla 4). Lo implementan {@code integracion.payu} y
 * {@code integracion.wompi}; la propiedad {@code nalu.pagos.pasarela} decide cuál está activa
 * (ADR 006). Cambiar de pasarela no toca la lógica de negocio.
 */
public interface PasarelaPagos {

    /**
     * Datos para que el navegador del cliente pague en la pasarela. Sin {@code campos}, el navegador
     * va a {@code urlPago} (Wompi); con campos, los envía a {@code urlPago} en un formulario POST (PayU).
     */
    record DatosPago(String urlPago, String referencia, long montoCentavos, String moneda, Map<String, String> campos) {}

    /**
     * Transacción informada por la pasarela, ya con la firma verificada. {@code estado} es
     * {@code APPROVED}, {@code DECLINED}, {@code VOIDED} o {@code PENDING}; cualquier otro valor es un error.
     */
    record EventoTransaccion(String tipo, String idTransaccion, String referencia, String estado, long montoCentavos,
                             String metodo, Map<String, Object> carga) {}

    DatosPago prepararPago(String referencia, long montoCentavos, String correoCliente, String urlRetorno);

    /** @throws FirmaInvalidaException si la firma del evento no es válida */
    EventoTransaccion verificarEvento(String cuerpo);
}
