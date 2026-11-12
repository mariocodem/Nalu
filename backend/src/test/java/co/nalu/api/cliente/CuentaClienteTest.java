package co.nalu.api.cliente;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.nalu.ApiTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** RF02: perfil y direcciones; cada cliente solo ve lo suyo (RNF02). */
class CuentaClienteTest extends ApiTestBase {

    private static final String DIRECCION = """
            {"etiqueta":"Casa","destinatario":"Ana Pérez","departamento":"Antioquia","ciudad":"Medellín",
             "direccion":"Calle 10 # 43-12","codigoPostal":"050021","telefono":"3001234567"}""";

    @Test
    void elClienteConsultaYActualizaSuPerfil() throws Exception {
        crearCliente("ana@nalu.co");
        var token = bearer("ana@nalu.co");

        mvc.perform(get("/api/v1/cuenta/perfil").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("ana@nalu.co"));
        mvc.perform(put("/api/v1/cuenta/perfil").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"Ana María","apellido":"Pérez","telefono":"+57 300 765 4321",
                                 "documento":"1020304050","aceptaComunicaciones":true}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana María"))
                .andExpect(jsonPath("$.telefono").value("+573007654321"))
                .andExpect(jsonPath("$.aceptaComunicaciones").value(true));
    }

    @Test
    void lasDireccionesSeCreanEditanYDesactivan() throws Exception {
        crearCliente("ana@nalu.co");
        var token = bearer("ana@nalu.co");

        var creada = mvc.perform(post("/api/v1/cuenta/direcciones").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(DIRECCION))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(creada, "$.id")).longValue();

        mvc.perform(put("/api/v1/cuenta/direcciones/" + id).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(DIRECCION.replace("Casa", "Oficina")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etiqueta").value("Oficina"));
        mvc.perform(delete("/api/v1/cuenta/direcciones/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/cuenta/direcciones").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void laDireccionDeOtroClienteNoExiste() throws Exception {
        crearCliente("ana@nalu.co");
        crearCliente("eva@nalu.co");
        var ana = bearer("ana@nalu.co");
        var creada = mvc.perform(post("/api/v1/cuenta/direcciones").header(HttpHeaders.AUTHORIZATION, ana)
                        .contentType(MediaType.APPLICATION_JSON).content(DIRECCION))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(creada, "$.id")).longValue();

        mvc.perform(put("/api/v1/cuenta/direcciones/" + id).header(HttpHeaders.AUTHORIZATION, bearer("eva@nalu.co"))
                        .contentType(MediaType.APPLICATION_JSON).content(DIRECCION))
                .andExpect(status().isNotFound());
    }

    @Test
    void direccionIncompletaResponde400() throws Exception {
        crearCliente("ana@nalu.co");
        mvc.perform(post("/api/v1/cuenta/direcciones").header(HttpHeaders.AUTHORIZATION, bearer("ana@nalu.co"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"etiqueta\":\"Casa\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.length()").value(5));
    }

    @Test
    void unAdministradorNoTienePerfilDeCliente() throws Exception {
        crearAdministrador("admin@nalu.co");
        mvc.perform(get("/api/v1/cuenta/perfil").header(HttpHeaders.AUTHORIZATION, bearer("admin@nalu.co")))
                .andExpect(status().isForbidden());
    }
}
