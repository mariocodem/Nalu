package co.nalu.datos.entidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.nalu.TestcontainersConfiguration;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Persiste las entidades de cuentas contra el esquema que crea Liquibase en un PostgreSQL 18 real
 * (Testcontainers), con {@code ddl-auto=validate}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class MapeoCuentasTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 11, 9, 15, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private TestEntityManager em;

    @Test
    void persisteClienteYAdministradorConHerenciaJoined() {
        var cliente = em.persistFlushFind(new Cliente("Ana", "Pérez", "ana@nalu.co", "hash", AHORA));
        var admin = em.persistFlushFind(new Administrador("Luis", "Gómez", "admin@nalu.co", "hash"));
        em.clear();

        Usuario leido = em.find(Usuario.class, cliente.getId());
        assertThat(leido).isInstanceOf(Cliente.class);
        assertThat(leido.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(leido.getFechaRegistro()).isNotNull();
        assertThat(((Cliente) leido).getPoliticaAceptadaEn()).isEqualTo(AHORA);
        assertThat(em.find(Usuario.class, admin.getId())).isInstanceOf(Administrador.class);
    }

    @Test
    void elCorreoEsUnico() {
        em.persistAndFlush(new Cliente("Ana", "Pérez", "repetido@nalu.co", "hash", AHORA));

        assertThatThrownBy(() -> em.persistAndFlush(new Cliente("Eva", "Ruiz", "repetido@nalu.co", "hash", AHORA)))
                .hasRootCauseInstanceOf(org.postgresql.util.PSQLException.class)
                .hasMessageContaining("uk_usuario_correo");
    }

    @Test
    void persisteLaRotacionDeRefreshTokens() {
        var cliente = em.persistAndFlush(new Cliente("Ana", "Pérez", "rt@nalu.co", "hash", AHORA));
        var original = em.persistAndFlush(RefreshToken.iniciarFamilia(cliente, "a".repeat(64), AHORA,
                Duration.ofMinutes(30), Duration.ofDays(7), "127.0.0.1", "JUnit"));
        var nuevo = em.persistAndFlush(original.rotar("b".repeat(64), AHORA.plusMinutes(5),
                Duration.ofMinutes(30), "127.0.0.1", "JUnit"));
        em.flush();
        em.clear();

        var leido = em.find(RefreshToken.class, original.getId());
        assertThat(leido.estaRevocado()).isTrue();
        assertThat(leido.getReemplazadoPor().getId()).isEqualTo(nuevo.getId());
        assertThat(leido.getFamilia()).isEqualTo(nuevo.getFamilia());
        assertThat(em.find(RefreshToken.class, nuevo.getId()).getLimiteAbsoluto()).isEqualTo(AHORA.plusDays(7));
    }
}
