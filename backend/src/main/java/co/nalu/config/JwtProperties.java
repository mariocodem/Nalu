package co.nalu.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Parámetros de los tokens JWT (plan de desarrollo §8.1 y §8.5).
 *
 * @param secreto clave HS256 en Base64; debe tener al menos 256 bits
 * @param emisor valor del claim {@code iss}
 * @param vidaAccessToken vida del access token
 * @param inactividadRefresh tiempo sin uso tras el que expira el refresh token
 * @param vidaMaximaRefresh vida máxima absoluta de una familia de refresh tokens
 */
@Validated
@ConfigurationProperties("nalu.jwt")
public record JwtProperties(
        @NotBlank String secreto,
        @NotBlank String emisor,
        @NotNull Duration vidaAccessToken,
        @NotNull Duration inactividadRefresh,
        @NotNull Duration vidaMaximaRefresh) {

    static final int BYTES_MINIMOS_CLAVE = 32;

    public JwtProperties {
        if (secreto != null && !secreto.isBlank()) {
            byte[] clave;
            try {
                clave = Base64.getDecoder().decode(secreto);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("nalu.jwt.secreto debe estar codificado en Base64", e);
            }
            if (clave.length < BYTES_MINIMOS_CLAVE) {
                throw new IllegalArgumentException(
                        "nalu.jwt.secreto debe tener al menos 256 bits (genérala con: openssl rand -base64 32)");
            }
        }
    }

    /** Clave de firma HS256 derivada de {@link #secreto()}. */
    public SecretKey claveSecreta() {
        return new SecretKeySpec(Base64.getDecoder().decode(secreto), "HmacSHA256");
    }

    /** Evita que la clave aparezca en logs o mensajes de error. */
    @Override
    public String toString() {
        return "JwtProperties[emisor=%s, vidaAccessToken=%s, inactividadRefresh=%s, vidaMaximaRefresh=%s]"
                .formatted(emisor, vidaAccessToken, inactividadRefresh, vidaMaximaRefresh);
    }
}
