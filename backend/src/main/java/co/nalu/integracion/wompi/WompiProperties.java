package co.nalu.integracion.wompi;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Llaves de Wompi (sandbox en desarrollo). Solo llegan por variables de entorno (P-04).
 *
 * @param urlCheckout checkout web de Wompi; el entorno (sandbox o producción) lo decide la llave
 */
@ConfigurationProperties("nalu.wompi")
public record WompiProperties(String llavePublica, String secretoIntegridad, String secretoEventos,
                              @DefaultValue("https://checkout.wompi.co/p/") String urlCheckout) {

    @Override
    public String toString() {
        return "WompiProperties[llavePublica=" + llavePublica + ", urlCheckout=" + urlCheckout + "]";
    }
}
