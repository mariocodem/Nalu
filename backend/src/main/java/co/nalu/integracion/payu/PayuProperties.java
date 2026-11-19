package co.nalu.integracion.payu;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cuenta de PayU Latam para WebCheckout (ADR 006). En {@code application.yml} los valores por defecto
 * son las credenciales públicas de prueba que PayU publica para su sandbox de Colombia; una cuenta
 * propia se configura con las variables {@code PAYU_*}. La API key solo vive en el servidor.
 *
 * @param urlCheckout pasarela a la que el navegador envía el formulario
 * @param prueba envía {@code test=1}: la transacción es de prueba
 */
@ConfigurationProperties("nalu.payu")
public record PayuProperties(String merchantId, String accountId, String apiKey,
                             @DefaultValue("https://sandbox.checkout.payulatam.com/ppp-web-gateway-payu/") String urlCheckout,
                             @DefaultValue("true") boolean prueba) {

    @Override
    public String toString() {
        return "PayuProperties[merchantId=" + merchantId + ", accountId=" + accountId + ", urlCheckout=" + urlCheckout
                + ", prueba=" + prueba + "]";
    }
}
