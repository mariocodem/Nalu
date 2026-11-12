package co.nalu.api.error;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import am.ik.yavi.core.Validator;
import co.nalu.negocio.comun.ConflictoException;
import co.nalu.negocio.comun.EsquemaValidacion;
import co.nalu.negocio.comun.RecursoNoEncontradoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(ManejadorErroresTest.ControladorPrueba.class)
@Import({ManejadorErroresTest.ControladorPrueba.class, ManejadorErrores.class})
@ActiveProfiles("test")
@WithMockUser
class ManejadorErroresTest {

    /** El interceptor de auditoría se carga con la capa web; aquí no hace falta su servicio real. */
    @MockitoBean
    private co.nalu.negocio.auditoria.AuditoriaService auditoria;

    record AltaPrueba(String nombre, String correo, Integer cantidad) {
        static final Validator<AltaPrueba> ESQUEMA = EsquemaValidacion.para(AltaPrueba.class)
                ._string(AltaPrueba::nombre, "nombre", c -> c.notBlank())
                ._string(AltaPrueba::correo, "correo", c -> c.notBlank().email())
                ._integer(AltaPrueba::cantidad, "cantidad", c -> c.notNull().greaterThan(0))
                .build();
    }

    @RestController
    static class ControladorPrueba {

        @PostMapping("/prueba/alta")
        AltaPrueba alta(@RequestBody AltaPrueba alta) {
            return Validacion.validar(alta, AltaPrueba.ESQUEMA);
        }

        @GetMapping("/prueba/no-encontrado")
        void noEncontrado() {
            throw new RecursoNoEncontradoException("El pedido no existe");
        }

        @GetMapping("/prueba/conflicto")
        void conflicto() {
            throw new ConflictoException("No hay existencias suficientes");
        }

        @GetMapping("/prueba/denegado")
        void denegado() {
            throw new AccessDeniedException("sin permiso");
        }

        @GetMapping("/prueba/fallo")
        void fallo() {
            throw new IllegalStateException("detalle interno que no debe salir");
        }
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void entradaInvalidaDevuelve400ConTodosLosErroresEnEspanol() throws Exception {
        mvc.perform(post("/prueba/alta").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"correo\":\"x\",\"cantidad\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:nalu:error:validacion"))
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errores[*].campo").value(containsInAnyOrder("nombre", "correo", "cantidad")))
                .andExpect(jsonPath("$.errores[*].mensaje").value(containsInAnyOrder(
                        "El campo «nombre» es obligatorio",
                        "El campo «correo» debe ser un correo electrónico válido",
                        "El campo «cantidad» debe ser mayor que 0")));
    }

    @Test
    void entradaValidaPasa() throws Exception {
        mvc.perform(post("/prueba/alta").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"correo\":\"ana@nalu.co\",\"cantidad\":2}"))
                .andExpect(status().isOk());
    }

    @Test
    void jsonMalFormadoDevuelve400EnEspanol() throws Exception {
        mvc.perform(post("/prueba/alta").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{nombre:"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Solicitud ilegible"))
                .andExpect(jsonPath("$.detail").value("El cuerpo de la solicitud falta o no es un JSON válido."));
    }

    @Test
    void recursoNoEncontradoDevuelve404() throws Exception {
        mvc.perform(get("/prueba/no-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:nalu:error:no-encontrado"))
                .andExpect(jsonPath("$.detail").value("El pedido no existe"));
    }

    @Test
    void conflictoDevuelve409() throws Exception {
        mvc.perform(get("/prueba/conflicto"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflicto"))
                .andExpect(jsonPath("$.detail").value("No hay existencias suficientes"));
    }

    @Test
    void rutaInexistenteDevuelve404EnEspanol() throws Exception {
        mvc.perform(get("/api/v1/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Ruta no encontrada"));
    }

    @Test
    void metodoNoPermitidoDevuelve405EnEspanol() throws Exception {
        mvc.perform(delete("/prueba/conflicto").with(csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.title").value("Método no permitido"))
                .andExpect(jsonPath("$.detail").value("El método DELETE no está permitido en esta ruta."));
    }

    @Test
    void accesoDenegadoLlegaASpringSecurityComo403() throws Exception {
        mvc.perform(get("/prueba/denegado")).andExpect(status().isForbidden());
    }

    @Test
    void errorInesperadoDevuelve500SinDetallesInternos() throws Exception {
        mvc.perform(get("/prueba/fallo"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("urn:nalu:error:interno"))
                .andExpect(content().string(not(containsString("detalle interno"))));
    }
}
