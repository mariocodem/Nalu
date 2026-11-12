package co.nalu.integracion.wompi;

import co.nalu.negocio.pagos.FirmaInvalidaException;
import co.nalu.negocio.pagos.PasarelaPagos;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptador de Wompi (RF12, RNF04): el cliente paga en el checkout web de Wompi, de modo que los
 * datos de la tarjeta nunca pasan por Nalu.
 *
 * <ul>
 *   <li>Checkout: la firma de integridad es SHA-256 de {@code referencia + montoEnCentavos +
 *       moneda + secretoDeIntegridad}.
 *   <li>Eventos: la firma es SHA-256 de los valores de {@code signature.properties} (leídos de
 *       {@code data}) concatenados + {@code timestamp} + secreto de eventos, comparada con
 *       {@code signature.checksum}.
 * </ul>
 */
@Component
public class WompiPasarela implements PasarelaPagos {

    static final String MONEDA = "COP";

    private final WompiProperties props;
    private final JsonMapper json;

    public WompiPasarela(WompiProperties props, JsonMapper json) {
        this.props = props;
        this.json = json;
    }

    @Override
    public DatosPago prepararPago(String referencia, long montoCentavos, String correoCliente, String urlRetorno) {
        var firma = sha256(referencia + montoCentavos + MONEDA + requerido(props.secretoIntegridad(), "secreto de integridad"));
        var parametros = new LinkedHashMap<String, String>();
        parametros.put("public-key", requerido(props.llavePublica(), "llave pública"));
        parametros.put("currency", MONEDA);
        parametros.put("amount-in-cents", Long.toString(montoCentavos));
        parametros.put("reference", referencia);
        parametros.put("signature:integrity", firma);
        parametros.put("redirect-url", urlRetorno);
        if (correoCliente != null) {
            parametros.put("customer-data:email", correoCliente);
        }
        var consulta = new StringBuilder();
        parametros.forEach((k, v) -> consulta.append(consulta.isEmpty() ? "?" : "&").append(k).append('=')
                .append(URLEncoder.encode(v, StandardCharsets.UTF_8)));
        return new DatosPago(props.urlCheckout() + consulta, referencia, montoCentavos, MONEDA);
    }

    @Override
    @SuppressWarnings("unchecked")
    public EventoTransaccion verificarEvento(String cuerpo) {
        Map<String, Object> evento;
        try {
            evento = json.readValue(cuerpo, Map.class);
        } catch (JacksonException e) {
            throw new FirmaInvalidaException("El evento no es un JSON válido");
        }
        var datos = (Map<String, Object>) evento.get("data");
        var firma = (Map<String, Object>) evento.get("signature");
        if (datos == null || firma == null || evento.get("timestamp") == null) {
            throw new FirmaInvalidaException("El evento no trae datos o firma");
        }
        var concatenado = new StringBuilder();
        for (var propiedad : (List<String>) firma.getOrDefault("properties", List.of())) {
            concatenado.append(valor(datos, propiedad));
        }
        concatenado.append(evento.get("timestamp")).append(requerido(props.secretoEventos(), "secreto de eventos"));
        var esperado = sha256(concatenado.toString());
        var recibido = String.valueOf(firma.get("checksum"));
        if (!MessageDigest.isEqual(esperado.getBytes(StandardCharsets.US_ASCII),
                recibido.toLowerCase().getBytes(StandardCharsets.US_ASCII))) {
            throw new FirmaInvalidaException("La firma del evento no es válida");
        }
        var transaccion = (Map<String, Object>) datos.get("transaction");
        if (transaccion == null) {
            throw new FirmaInvalidaException("El evento no es de una transacción");
        }
        return new EventoTransaccion(String.valueOf(evento.get("event")), String.valueOf(transaccion.get("id")),
                String.valueOf(transaccion.get("reference")), String.valueOf(transaccion.get("status")),
                ((Number) transaccion.get("amount_in_cents")).longValue(),
                transaccion.get("payment_method_type") == null ? null : String.valueOf(transaccion.get("payment_method_type")),
                evento);
    }

    /** Lee una ruta con puntos (p. ej. {@code transaction.amount_in_cents}) dentro de {@code data}. */
    @SuppressWarnings("unchecked")
    private static String valor(Map<String, Object> datos, String ruta) {
        Object actual = datos;
        for (var parte : ruta.split("\\.")) {
            if (!(actual instanceof Map<?, ?> mapa)) {
                throw new FirmaInvalidaException("Propiedad firmada inexistente: " + ruta);
            }
            actual = ((Map<String, Object>) mapa).get(parte);
        }
        return actual == null ? "" : String.valueOf(actual);
    }

    static String sha256(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String requerido(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta configurar el " + nombre + " de Wompi (variables WOMPI_* en .env)");
        }
        return valor;
    }
}
