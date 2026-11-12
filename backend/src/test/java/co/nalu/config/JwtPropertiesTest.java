package co.nalu.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

class JwtPropertiesTest {

    private static final String CLAVE_256_BITS = "bmFsdS1jbGF2ZS1kZS1wcnVlYmFzLTI1Ni1iaXRzISE=";

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(PropiedadesConfig.class)
            .withPropertyValues("nalu.app.url-frontend=http://localhost:4200");

    @Test
    void enlazaLosValoresYConvierteLasDuraciones() {
        contexto.withPropertyValues(
                        "nalu.jwt.secreto=" + CLAVE_256_BITS,
                        "nalu.jwt.emisor=http://localhost:8080",
                        "nalu.jwt.vida-access-token=15m",
                        "nalu.jwt.inactividad-refresh=30m",
                        "nalu.jwt.vida-maxima-refresh=7d")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    var props = ctx.getBean(JwtProperties.class);
                    assertThat(props.vidaAccessToken()).isEqualTo(Duration.ofMinutes(15));
                    assertThat(props.vidaMaximaRefresh()).isEqualTo(Duration.ofDays(7));
                    assertThat(props.claveSecreta().getEncoded()).hasSize(32);
                    assertThat(props.toString()).doesNotContain(CLAVE_256_BITS);
                });
    }

    @Test
    void rechazaUnaClaveDeMenosDe256Bits() {
        contexto.withPropertyValues(
                        "nalu.jwt.secreto=Y29ydGE=", // "corta"
                        "nalu.jwt.emisor=http://localhost:8080",
                        "nalu.jwt.vida-access-token=15m",
                        "nalu.jwt.inactividad-refresh=30m",
                        "nalu.jwt.vida-maxima-refresh=7d")
                .run(ctx -> assertThat(ctx).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("al menos 256 bits"));
    }

    @Test
    void rechazaLaConfiguracionSinEmisor() {
        contexto.withPropertyValues(
                        "nalu.jwt.secreto=" + CLAVE_256_BITS,
                        "nalu.jwt.vida-access-token=15m",
                        "nalu.jwt.inactividad-refresh=30m",
                        "nalu.jwt.vida-maxima-refresh=7d")
                .run(ctx -> assertThat(ctx).hasFailed());
    }
}
