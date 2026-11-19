package co.nalu.integracion.payu;

import co.nalu.negocio.pagos.FirmaInvalidaException;
import co.nalu.negocio.pagos.PasarelaPagos;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptador de PayU Latam WebCheckout (ADR 006): el navegador envía un formulario POST a PayU, de
 * modo que los datos de la tarjeta nunca pasan por Nalu.
 *
 * <ul>
 *   <li>Formulario: {@code signature} es MD5 de {@code ApiKey~merchantId~referenceCode~amount~currency}.
 *   <li>Respuesta: PayU devuelve al cliente a {@code responseUrl} con el resultado en la URL (GET) y el
 *       frontend reenvía esos parámetros. {@code signature} es MD5 de
 *       {@code ApiKey~merchantId~referenceCode~TX_VALUE~currency~transactionState}, con {@code TX_VALUE}
 *       redondeado a un decimal con el método mitad al par.
 * </ul>
 *
 * No se usa la página de confirmación de PayU (webhook) porque exige una URL pública.
 */
@Component
@ConditionalOnProperty(name = "nalu.pagos.pasarela", havingValue = "payu", matchIfMissing = true)
public class PayuPasarela implements PasarelaPagos {

    static final String MONEDA = "COP";

    private final PayuProperties props;
    private final JsonMapper json;

    public PayuPasarela(PayuProperties props, JsonMapper json) {
        this.props = props;
        this.json = json;
    }

    @Override
    public DatosPago prepararPago(String referencia, long montoCentavos, String correoCliente, String urlRetorno) {
        // 11500000 centavos → "115000": los pesos colombianos van sin decimales.
        var monto = BigDecimal.valueOf(montoCentavos, 2).stripTrailingZeros().toPlainString();
        var campos = new LinkedHashMap<String, String>();
        campos.put("merchantId", requerido(props.merchantId(), "merchant id"));
        campos.put("accountId", requerido(props.accountId(), "account id"));
        campos.put("description", "Compra en Nalu, referencia " + referencia);
        campos.put("referenceCode", referencia);
        campos.put("amount", monto);
        campos.put("tax", "0");
        campos.put("taxReturnBase", "0");
        campos.put("currency", MONEDA);
        campos.put("signature", firma(referencia, monto, MONEDA));
        campos.put("test", props.prueba() ? "1" : "0");
        if (correoCliente != null) {
            campos.put("buyerEmail", correoCliente);
        }
        campos.put("responseUrl", urlRetorno);
        return new DatosPago(props.urlCheckout(), referencia, montoCentavos, MONEDA, Collections.unmodifiableMap(campos));
    }

    /** Verifica los parámetros de la página de respuesta que el frontend reenvía como JSON. */
    @Override
    @SuppressWarnings("unchecked")
    public EventoTransaccion verificarEvento(String cuerpo) {
        Map<String, Object> respuesta;
        try {
            respuesta = json.readValue(cuerpo, Map.class);
        } catch (JacksonException e) {
            throw new FirmaInvalidaException("La respuesta de PayU no es un JSON válido");
        }
        var referencia = requeridoEn(respuesta, "referenceCode");
        var valor = requeridoEn(respuesta, "TX_VALUE");
        var moneda = requeridoEn(respuesta, "currency");
        var estado = requeridoEn(respuesta, "transactionState");
        var idTransaccion = requeridoEn(respuesta, "transactionId");
        var recibida = requeridoEn(respuesta, "signature");

        BigDecimal monto;
        try {
            monto = new BigDecimal(valor);
        } catch (NumberFormatException e) {
            throw new FirmaInvalidaException("TX_VALUE no es un número");
        }
        var esperada = firma(referencia, monto.setScale(1, RoundingMode.HALF_EVEN).toPlainString(), moneda, estado);
        if (!MessageDigest.isEqual(esperada.getBytes(StandardCharsets.US_ASCII),
                recibida.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII))) {
            throw new FirmaInvalidaException("La firma de la respuesta de PayU no es válida");
        }
        long centavos;
        try {
            centavos = monto.movePointRight(2).longValueExact();
        } catch (ArithmeticException e) {
            throw new FirmaInvalidaException("TX_VALUE tiene más de dos decimales");
        }
        var metodo = respuesta.get("lapPaymentMethod");
        return new EventoTransaccion("payu.respuesta", idTransaccion, referencia, traducir(estado), centavos,
                metodo == null ? null : String.valueOf(metodo), respuesta);
    }

    /** {@code transactionState} de PayU en el vocabulario del puerto. 5 (expirada) y 104 (error) no son pagos. */
    static String traducir(String transactionState) {
        return switch (transactionState) {
            case "4" -> "APPROVED";
            case "6" -> "DECLINED";
            case "7" -> "PENDING";
            default -> "ERROR";
        };
    }

    /** MD5 de la API key, el merchant id y los valores dados, separados por {@code ~}. */
    private String firma(String... valores) {
        var cadena = new StringBuilder(requerido(props.apiKey(), "API key")).append('~').append(props.merchantId());
        for (var valor : valores) {
            cadena.append('~').append(valor);
        }
        return md5(cadena.toString());
    }

    static String md5(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String requeridoEn(Map<String, Object> respuesta, String parametro) {
        var valor = respuesta.get(parametro);
        if (valor == null || String.valueOf(valor).isBlank()) {
            throw new FirmaInvalidaException("Falta el parámetro " + parametro + " en la respuesta de PayU");
        }
        return String.valueOf(valor);
    }

    private static String requerido(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta configurar el " + nombre + " de PayU (variables PAYU_* en .env)");
        }
        return valor;
    }
}
