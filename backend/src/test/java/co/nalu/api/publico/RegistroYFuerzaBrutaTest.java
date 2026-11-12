package co.nalu.api.publico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.nalu.TestcontainersConfiguration;
import co.nalu.datos.entidad.Cliente;
import co.nalu.datos.repositorio.IntentoLoginRepository;
import co.nalu.datos.repositorio.RefreshTokenRepository;
import co.nalu.datos.repositorio.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** RF01: registro de clientes y bloqueo del login por fuerza bruta (docs/arquitectura/api-fase1.md §1). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class RegistroYFuerzaBrutaTest {

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private RefreshTokenRepository refreshTokens;
    @Autowired private co.nalu.datos.repositorio.AuditoriaRepository auditoria;
    @Autowired private IntentoLoginRepository intentos;

    @BeforeEach
    void limpiar() {
        auditoria.deleteAllInBatch();
        refreshTokens.deleteAllInBatch();
        intentos.deleteAllInBatch();
        usuarios.deleteAll();
    }

    @Test
    void registroCreaElClienteYDejaLaSesionIniciada() throws Exception {
        mvc.perform(registro(" Eva@Nalu.co ", "+573001234567", "secreto-123", true))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.usuario.correo").value("eva@nalu.co"))
                .andExpect(jsonPath("$.usuario.roles[0]").value("CLIENTE"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, startsWith("nalu_rt=")));

        var cliente = (Cliente) usuarios.findByCorreo("eva@nalu.co").orElseThrow();
        assertThat(cliente.getTelefono()).isEqualTo("+573001234567");
        assertThat(cliente.getPoliticaAceptadaEn()).isNotNull();
        assertThat(cliente.getContrasenaHash()).startsWith("$2").doesNotContain("secreto-123");
    }

    @Test
    void sinAceptarLaPoliticaResponde400() throws Exception {
        mvc.perform(registro("eva@nalu.co", "3001234567", "secreto-123", false))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo").value(hasItem("aceptaPolitica")));
    }

    @Test
    void datosInvalidosResponden400ConCadaCampo() throws Exception {
        mvc.perform(registro("no-es-correo", "12", "corta", true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo").value(hasItem("correo")))
                .andExpect(jsonPath("$.errores[*].campo").value(hasItem("telefono")))
                .andExpect(jsonPath("$.errores[*].campo").value(hasItem("contrasena")));
    }

    @Test
    void correoRepetidoResponde409() throws Exception {
        mvc.perform(registro("eva@nalu.co", "3001234567", "secreto-123", true)).andExpect(status().isCreated());

        mvc.perform(registro("EVA@nalu.co", "3001234567", "otro-secreto-1", true))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe una cuenta con ese correo"));
    }

    @Test
    void cincoFallosBloqueanElCorreoAunqueLaSextaContrasenaSeaCorrecta() throws Exception {
        mvc.perform(registro("eva@nalu.co", "3001234567", "secreto-123", true)).andExpect(status().isCreated());

        for (int i = 0; i < 5; i++) {
            mvc.perform(login("eva@nalu.co", "incorrecta")).andExpect(status().isUnauthorized());
        }
        mvc.perform(login("eva@nalu.co", "secreto-123"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value("Demasiados intentos fallidos. Espera unos minutos e inténtalo de nuevo."));
        // Otro correo no se ve afectado.
        mvc.perform(login("otra@nalu.co", "incorrecta")).andExpect(status().isUnauthorized());
        // Los intentos durante el bloqueo no se registran.
        assertThat(intentos.count()).isEqualTo(6);
    }

    @Test
    void unLoginCorrectoReiniciaLaCuentaDeFallos() throws Exception {
        mvc.perform(registro("eva@nalu.co", "3001234567", "secreto-123", true)).andExpect(status().isCreated());

        for (int i = 0; i < 4; i++) {
            mvc.perform(login("eva@nalu.co", "incorrecta")).andExpect(status().isUnauthorized());
        }
        mvc.perform(login("eva@nalu.co", "secreto-123")).andExpect(status().isOk());
        for (int i = 0; i < 4; i++) {
            mvc.perform(login("eva@nalu.co", "incorrecta")).andExpect(status().isUnauthorized());
        }
        mvc.perform(login("eva@nalu.co", "secreto-123")).andExpect(status().isOk());
    }

    private static MockHttpServletRequestBuilder registro(String correo, String telefono, String contrasena,
                                                          boolean aceptaPolitica) {
        return post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
                {"nombre":"Eva","apellido":"Ruiz","correo":"%s","telefono":"%s","contrasena":"%s",
                 "aceptaPolitica":%s,"aceptaComunicaciones":false}""".formatted(correo, telefono, contrasena, aceptaPolitica));
    }

    private static MockHttpServletRequestBuilder login(String correo, String contrasena) {
        return post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, contrasena));
    }
}
