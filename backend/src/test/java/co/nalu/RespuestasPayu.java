package co.nalu;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Parámetros de la página de respuesta de PayU, firmados como los firma PayU con las credenciales
 * públicas del sandbox (las de {@code application.yml}).
 */
public final class RespuestasPayu {

    public static final String API_KEY = "4Vj8eK4rloUd272L48hsrarnUA";
    public static final String MERCHANT_ID = "508029";

    private RespuestasPayu() {}

    /**
     * Respuesta de un pago en pesos enteros: PayU envía {@code TX_VALUE} con dos decimales
     * ({@code 115000.00}) y lo firma con uno ({@code 115000.0}).
     */
    public static String respuesta(String referencia, long centavos, String transactionState, String idTransaccion) {
        if (centavos % 100 != 0) {
            throw new IllegalArgumentException("Solo pesos enteros: " + centavos);
        }
        long pesos = centavos / 100;
        var firma = md5(API_KEY + "~" + MERCHANT_ID + "~" + referencia + "~" + pesos + ".0~COP~" + transactionState);
        return """
                {"merchantId":"%s","transactionState":"%s","message":"APPROVED","referenceCode":"%s",
                 "reference_pol":"8888001","transactionId":"%s","TX_VALUE":"%d.00","currency":"COP",
                 "lapPaymentMethod":"VISA","buyerEmail":"ana@nalu.co","signature":"%s"}"""
                .formatted(MERCHANT_ID, transactionState, referencia, idTransaccion, pesos, firma);
    }

    public static String md5(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
