package co.nalu.datos.entidad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de las reglas de estado de {@link RefreshToken}, sin Spring ni base de datos.
 */
class RefreshTokenTest {

    private static final OffsetDateTime LOGIN = OffsetDateTime.parse("2026-11-09T10:00:00Z");
    private static final Duration INACTIVIDAD = Duration.ofMinutes(30);
    private static final Duration VIDA_MAXIMA = Duration.ofDays(7);

    private final Usuario usuario = new Administrador("Ana", "Pérez", "ana@nalu.local", "hash-de-prueba");

    private RefreshToken login() {
        return RefreshToken.iniciarFamilia(usuario, "a".repeat(64), LOGIN, INACTIVIDAD, VIDA_MAXIMA,
                "127.0.0.1", "JUnit");
    }

    @Test
    void elLoginAbreUnaFamiliaQueExpiraPorInactividad() {
        RefreshToken token = login();

        assertThat(token.getExpiraEn()).isEqualTo(LOGIN.plus(INACTIVIDAD));
        assertThat(token.getLimiteAbsoluto()).isEqualTo(LOGIN.plus(VIDA_MAXIMA));
        assertThat(token.estaVigente(LOGIN.plusMinutes(29))).isTrue();
        assertThat(token.estaVigente(LOGIN.plusMinutes(30))).isFalse();
    }

    @Test
    void unTokenRevocadoNoEstaVigente() {
        RefreshToken token = login();

        token.revocar(LOGIN.plusMinutes(5));

        assertThat(token.estaRevocado()).isTrue();
        assertThat(token.estaVigente(LOGIN.plusMinutes(6))).isFalse();
    }

    @Test
    void revocarEsIdempotenteYNoSobrescribeLaFechaOriginal() {
        RefreshToken token = login();

        token.revocar(LOGIN.plusMinutes(5));
        token.revocar(LOGIN.plusMinutes(10));

        assertThat(token.getRevocadoEn()).isEqualTo(LOGIN.plusMinutes(5));
    }

    @Test
    void rotarRevocaElTokenYDevuelveUnSucesorDeLaMismaFamilia() {
        RefreshToken original = login();
        OffsetDateTime uso = LOGIN.plusMinutes(20);

        RefreshToken sucesor = original.rotar("b".repeat(64), uso, INACTIVIDAD, "127.0.0.2", "JUnit 2");

        assertThat(original.estaRevocado()).isTrue();
        assertThat(original.getReemplazadoPor()).isSameAs(sucesor);
        assertThat(sucesor.getFamilia()).isEqualTo(original.getFamilia());
        assertThat(sucesor.getUsuario()).isSameAs(usuario);
        assertThat(sucesor.getExpiraEn()).isEqualTo(uso.plus(INACTIVIDAD));
        assertThat(sucesor.estaVigente(uso.plusMinutes(29))).isTrue();
    }

    @Test
    void lasRotacionesNoExtiendenElTopeAbsolutoDeLaSesion() {
        RefreshToken token = login();
        // Un uso cada 25 minutos durante casi 7 días mantiene viva la sesión…
        OffsetDateTime ahora = LOGIN;
        while (ahora.plusMinutes(25).isBefore(LOGIN.plus(VIDA_MAXIMA).minusMinutes(10))) {
            ahora = ahora.plusMinutes(25);
            token = token.rotar("c".repeat(64), ahora, INACTIVIDAD, null, null);
        }

        // …pero el último token expira en el tope absoluto, no 30 minutos después del último uso.
        assertThat(token.getLimiteAbsoluto()).isEqualTo(LOGIN.plus(VIDA_MAXIMA));
        assertThat(token.getExpiraEn()).isEqualTo(LOGIN.plus(VIDA_MAXIMA));
        assertThat(token.estaVigente(LOGIN.plus(VIDA_MAXIMA))).isFalse();
    }
}
