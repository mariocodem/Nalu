package co.nalu.api.publico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.nalu.TestcontainersConfiguration;
import co.nalu.config.JwtProperties;
import co.nalu.datos.entidad.Administrador;
import co.nalu.datos.entidad.Cliente;
import co.nalu.datos.repositorio.RefreshTokenRepository;
import co.nalu.datos.repositorio.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Flujo completo de autenticación contra PostgreSQL real (plan §14: JWT expirado o alterado → 401,
 * rotación del refresh, revocación de la familia por reutilización, inactividad de 30 min y
 * control de acceso a /admin/**).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, AutenticacionTest.RelojDePrueba.class})
@ActiveProfiles("test")
class AutenticacionTest {

    private static final String FRONTEND = "http://localhost:4200";
    private static final String CONTRASENA = "contrasena-segura-123";

    @TestConfiguration(proxyBeanMethods = false)
    static class RelojDePrueba {
        @Bean
        @Primary
        RelojAjustable relojAjustable() {
            return new RelojAjustable();
        }
    }

    /** Reloj que las pruebas pueden adelantar para simular inactividad. */
    static class RelojAjustable extends Clock {
        private volatile Instant ahora = Instant.now();

        void adelantar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        void reiniciar() {
            ahora = Instant.now();
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private RefreshTokenRepository refreshTokens;
    @Autowired private co.nalu.datos.repositorio.AuditoriaRepository auditoria;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtEncoder jwtEncoder;
    @Autowired private JwtProperties jwtProperties;
    @Autowired private RelojAjustable reloj;

    @BeforeEach
    void prepararDatos() {
        reloj.reiniciar();
        auditoria.deleteAllInBatch();
        refreshTokens.deleteAllInBatch();
        usuarios.deleteAll();
        usuarios.save(new Cliente("Ana", "Pérez", "ana@nalu.co", passwordEncoder.encode(CONTRASENA),
                OffsetDateTime.now()));
        usuarios.save(new Administrador("Luis", "Gómez", "admin@nalu.co", passwordEncoder.encode(CONTRASENA)));
    }

    // --- Login -------------------------------------------------------------------------------

    @Test
    void loginCorrectoDevuelveLaSesionYLaCookieDelRefreshToken() throws Exception {
        mvc.perform(login("  ANA@nalu.co ", CONTRASENA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiraEn").isNotEmpty())
                .andExpect(jsonPath("$.usuario.correo").value("ana@nalu.co"))
                .andExpect(jsonPath("$.usuario.roles[0]").value("CLIENTE"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, startsWith("nalu_rt=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=1800")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")));

        assertThat(refreshTokens.findAll()).singleElement()
                .satisfies(token -> assertThat(token.getHashToken()).hasSize(64));
    }

    @Test
    void contrasenaIncorrectaYCorreoInexistenteResponden401Iguales() throws Exception {
        for (var correo : List.of("ana@nalu.co", "nadie@nalu.co")) {
            mvc.perform(login(correo, "otra-contrasena"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos"));
        }
    }

    @Test
    void loginConDatosInvalidosResponde400ConLosErrores() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"no-es-correo\",\"contrasena\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.length()").value(2));
    }

    // --- Access token --------------------------------------------------------------------------

    @Test
    void meDevuelveElUsuarioDelToken() throws Exception {
        var token = accessToken(mvc.perform(login("ana@nalu.co", CONTRASENA)).andReturn());

        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.roles[0]").value("CLIENTE"));
    }

    @Test
    void sinTokenResponde401EnProblemJson() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("No autenticado"));
    }

    @Test
    void tokenAlteradoResponde401() throws Exception {
        var token = accessToken(mvc.perform(login("ana@nalu.co", CONTRASENA)).andReturn());
        var alterado = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + alterado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("El token de acceso no es válido o ha expirado."));
    }

    @Test
    void tokenExpiradoResponde401() throws Exception {
        var id = usuarios.findByCorreo("ana@nalu.co").orElseThrow().getId();
        var hace = Instant.now().minus(Duration.ofMinutes(20));
        var claims = JwtClaimsSet.builder().issuer(jwtProperties.emisor()).subject(id.toString())
                .issuedAt(hace).expiresAt(hace.plus(Duration.ofMinutes(15))).claim("roles", List.of("CLIENTE"))
                .build();
        var expirado = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expirado))
                .andExpect(status().isUnauthorized());
    }

    // --- Refresh token -------------------------------------------------------------------------

    @Test
    void refreshRotaElTokenYLaReutilizacionRevocaTodaLaFamilia() throws Exception {
        var original = cookie(mvc.perform(login("ana@nalu.co", CONTRASENA)).andReturn());

        var rotado = cookie(mvc.perform(refresh(original))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.usuario.correo").value("ana@nalu.co"))
                .andReturn());
        assertThat(rotado.getValue()).isNotEqualTo(original.getValue());

        // Alguien reutiliza el token ya rotado: se asume robo…
        mvc.perform(refresh(original)).andExpect(status().isUnauthorized());
        // …y también deja de valer el token legítimo de la misma familia.
        mvc.perform(refresh(rotado)).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshExigeElOrigenDelFrontend() throws Exception {
        var cookie = cookie(mvc.perform(login("ana@nalu.co", CONTRASENA)).andReturn());

        mvc.perform(post("/api/v1/auth/refresh").cookie(cookie))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/refresh").cookie(cookie).header(HttpHeaders.ORIGIN, "https://ataque.example"))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshSinCookieResponde401() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh").header(HttpHeaders.ORIGIN, FRONTEND))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Tu sesión expiró. Inicia sesión de nuevo."));
    }

    @Test
    void tras30MinutosSinUsoElRefreshTokenCaduca() throws Exception {
        var cookie = cookie(mvc.perform(login("ana@nalu.co", CONTRASENA)).andReturn());

        reloj.adelantar(Duration.ofMinutes(29));
        var rotado = cookie(mvc.perform(refresh(cookie)).andExpect(status().isOk()).andReturn());

        reloj.adelantar(Duration.ofMinutes(31));
        mvc.perform(refresh(rotado)).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevocaLaSesionYBorraLaCookie() throws Exception {
        var cookie = cookie(mvc.perform(login("ana@nalu.co", CONTRASENA)).andReturn());

        mvc.perform(post("/api/v1/auth/logout").cookie(cookie).header(HttpHeaders.ORIGIN, FRONTEND))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        mvc.perform(refresh(cookie)).andExpect(status().isUnauthorized());
    }

    // --- Autorización y CORS -------------------------------------------------------------------

    @Test
    void rutasAdminExigenElRolAdministrador() throws Exception {
        var cliente = accessToken(mvc.perform(login("ana@nalu.co", CONTRASENA)).andReturn());
        var admin = accessToken(mvc.perform(login("admin@nalu.co", CONTRASENA)).andReturn());

        mvc.perform(get("/api/v1/admin/no-existe")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/no-existe").header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Acceso denegado"));
        // Pasa la seguridad; la ruta todavía no existe.
        mvc.perform(get("/api/v1/admin/no-existe").header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void corsSoloAdmiteElOrigenDelFrontend() throws Exception {
        mvc.perform(options("/api/v1/auth/login").header(HttpHeaders.ORIGIN, FRONTEND)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

        mvc.perform(options("/api/v1/auth/login").header(HttpHeaders.ORIGIN, "https://ataque.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthEsPublico() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    // --- Utilidades ----------------------------------------------------------------------------

    private static MockHttpServletRequestBuilder login(String correo, String contrasena) {
        return post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, contrasena));
    }

    private static MockHttpServletRequestBuilder refresh(Cookie cookie) {
        return post("/api/v1/auth/refresh").cookie(cookie).header(HttpHeaders.ORIGIN, FRONTEND);
    }

    private static String accessToken(MvcResult resultado) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.accessToken");
    }

    private static Cookie cookie(MvcResult resultado) {
        var cookie = resultado.getResponse().getCookie("nalu_rt");
        assertThat(cookie).as("cookie nalu_rt").isNotNull();
        return new Cookie(cookie.getName(), cookie.getValue());
    }
}
