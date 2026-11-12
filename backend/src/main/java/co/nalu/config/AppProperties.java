package co.nalu.config;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Parámetros generales de la aplicación.
 *
 * @param urlFrontend origen del frontend: único origen permitido por CORS y el que se verifica en
 *     el header {@code Origin} de {@code /auth/refresh} y {@code /auth/logout} (plan §8.5)
 * @param cookieSegura marca la cookie del refresh token como {@code Secure} (solo HTTPS); se
 *     desactiva únicamente en el perfil {@code dev}
 */
@Validated
@ConfigurationProperties("nalu.app")
public record AppProperties(@NotNull URI urlFrontend, @DefaultValue("true") boolean cookieSegura) {

    /** Origen en la forma en que lo envía el navegador: {@code esquema://host[:puerto]}. */
    public String origenFrontend() {
        var puerto = urlFrontend.getPort() == -1 ? "" : ":" + urlFrontend.getPort();
        return urlFrontend.getScheme() + "://" + urlFrontend.getHost() + puerto;
    }
}
