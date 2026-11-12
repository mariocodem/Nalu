package co.nalu.integracion.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import co.nalu.negocio.asistente.AsistenteNoDisponibleException;
import co.nalu.negocio.asistente.ModeloConversacional.Autor;
import co.nalu.negocio.asistente.ModeloConversacional.Herramienta;
import co.nalu.negocio.asistente.ModeloConversacional.Mensaje;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/** Formato de la API de Gemini (probado contra el servicio real el 2026-11-12) y manejo de fallos. */
class GeminiModeloConversacionalTest {

    private static final String BASE = "https://gemini.test/v1beta";
    private static final List<Herramienta> HERRAMIENTAS = List.of(new Herramienta("buscar_piezas", "Busca piezas",
            Map.of("type", "object", "properties", Map.of("texto", Map.of("type", "string")))));
    private static final List<Mensaje> CONVERSACION = List.of(new Mensaje(Autor.CLIENTE, "¿Tienen anillos con esmeralda?"));

    private final JsonMapper json = JsonMapper.builder().build();
    private final RelojMovil reloj = new RelojMovil();

    /** Reloj que la prueba adelanta a mano. */
    static final class RelojMovil extends Clock {
        Instant ahora = Instant.parse("2026-11-12T15:00:00Z");

        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zona) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    private GeminiModeloConversacional modelo(MockRestServiceServer[] servidor, int maxRondas) {
        var builder = RestClient.builder();
        servidor[0] = MockRestServiceServer.bindTo(builder).build();
        var props = new GeminiProperties("llave-de-prueba", "principal", List.of("respaldo"), BASE,
                Duration.ofSeconds(5), maxRondas);
        return new GeminiModeloConversacional(props, json, builder, reloj);
    }

    private static String url(String modelo) {
        return BASE + "/models/" + modelo + ":generateContent";
    }

    private static String texto(String texto) {
        return """
                {"candidates":[{"content":{"role":"model","parts":[{"text":"pensando…","thought":true},{"text":"%s"}]},
                 "finishReason":"STOP"}]}""".formatted(texto);
    }

    private static final String LLAMADA = """
            {"candidates":[{"content":{"role":"model","parts":[{"functionCall":{"name":"buscar_piezas",
             "args":{"texto":"esmeralda"},"id":"call_1"},"thoughtSignature":"firma-xyz"}]},"finishReason":"STOP"}]}""";

    @Test
    void devuelveElTurnoDelModeloConSuFirmaYLaRespuestaConElIdDeLaLlamada() {
        var servidor = new MockRestServiceServer[1];
        var modelo = modelo(servidor, 6);
        servidor[0].expect(requestTo(url("principal"))).andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "llave-de-prueba"))
                .andExpect(jsonPath("$.systemInstruction.parts[0].text").value("reglas"))
                .andExpect(jsonPath("$.tools[0].functionDeclarations[0].name").value("buscar_piezas"))
                .andRespond(withSuccess(LLAMADA, MediaType.APPLICATION_JSON));
        servidor[0].expect(requestTo(url("principal")))
                .andExpect(jsonPath("$.contents[1].role").value("model"))
                .andExpect(jsonPath("$.contents[1].parts[0].thoughtSignature").value("firma-xyz"))
                .andExpect(jsonPath("$.contents[2].role").value("user"))
                .andExpect(jsonPath("$.contents[2].parts[0].functionResponse.id").value("call_1"))
                .andExpect(jsonPath("$.contents[2].parts[0].functionResponse.response.piezas[0]").value("Anillo Trilogía"))
                .andRespond(withSuccess(texto("Tenemos el Anillo Trilogía."), MediaType.APPLICATION_JSON));

        var llamadas = new ArrayList<Map<String, Object>>();
        var respuesta = modelo.responder("reglas", CONVERSACION, HERRAMIENTAS, (nombre, args) -> {
            llamadas.add(args);
            return Map.of("piezas", List.of("Anillo Trilogía"));
        });

        assertThat(respuesta).isEqualTo("Tenemos el Anillo Trilogía.");
        assertThat(llamadas).containsExactly(Map.of("texto", "esmeralda"));
        servidor[0].verify();
    }

    @Test
    void siElModeloPrincipalEstaSaturadoUsaElDeRespaldo() {
        var servidor = new MockRestServiceServer[1];
        var modelo = modelo(servidor, 6);
        servidor[0].expect(requestTo(url("principal"))).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\":{\"message\":\"high demand\"}}"));
        servidor[0].expect(requestTo(url("respaldo"))).andRespond(withSuccess(texto("Hola"), MediaType.APPLICATION_JSON));

        assertThat(modelo.responder("reglas", CONVERSACION, HERRAMIENTAS, (n, a) -> Map.of())).isEqualTo("Hola");
        servidor[0].verify();
    }

    @Test
    void unModeloSaturadoQuedaEnPausaYLuegoSeVuelveAProbar() {
        var servidor = new MockRestServiceServer[1];
        var modelo = modelo(servidor, 6);
        servidor[0].expect(requestTo(url("principal"))).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        servidor[0].expect(requestTo(url("respaldo"))).andRespond(withSuccess(texto("Uno"), MediaType.APPLICATION_JSON));
        // El turno siguiente va directo al respaldo: el principal sigue en pausa
        servidor[0].expect(requestTo(url("respaldo"))).andRespond(withSuccess(texto("Dos"), MediaType.APPLICATION_JSON));
        // Vencida la pausa, el principal se vuelve a probar
        servidor[0].expect(requestTo(url("principal"))).andRespond(withSuccess(texto("Tres"), MediaType.APPLICATION_JSON));

        assertThat(modelo.responder("reglas", CONVERSACION, HERRAMIENTAS, (n, a) -> Map.of())).isEqualTo("Uno");
        reloj.ahora = reloj.ahora.plusSeconds(60);
        assertThat(modelo.responder("reglas", CONVERSACION, HERRAMIENTAS, (n, a) -> Map.of())).isEqualTo("Dos");
        reloj.ahora = reloj.ahora.plus(GeminiModeloConversacional.PAUSA);
        assertThat(modelo.responder("reglas", CONVERSACION, HERRAMIENTAS, (n, a) -> Map.of())).isEqualTo("Tres");
        servidor[0].verify();
    }

    @Test
    void aMitadDeTurnoNoCambiaDeModeloPorqueLasFirmasSonDelModeloQueLasProdujo() {
        var servidor = new MockRestServiceServer[1];
        var modelo = modelo(servidor, 6);
        servidor[0].expect(requestTo(url("principal"))).andRespond(withSuccess(LLAMADA, MediaType.APPLICATION_JSON));
        servidor[0].expect(requestTo(url("principal"))).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        servidor[0].expect(requestTo(url("principal"))).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> modelo.responder("reglas", CONVERSACION, HERRAMIENTAS, (n, a) -> Map.of()))
                .isInstanceOf(AsistenteNoDisponibleException.class);
        servidor[0].verify();
    }

    @Test
    void unRechazoQueNoEsTransitorioNoSeReintentaEnOtroModelo() {
        var servidor = new MockRestServiceServer[1];
        var modelo = modelo(servidor, 6);
        servidor[0].expect(requestTo(url("principal"))).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> modelo.responder("reglas", CONVERSACION, HERRAMIENTAS, (n, a) -> Map.of()))
                .isInstanceOf(AsistenteNoDisponibleException.class);
        servidor[0].verify();
    }

    @Test
    void unaRespuestaBloqueadaPorSeguridadSeReemplazaPorUnMensajeAmable() {
        var servidor = new MockRestServiceServer[1];
        var modelo = modelo(servidor, 6);
        servidor[0].expect(requestTo(url("principal"))).andRespond(withSuccess(
                "{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}", MediaType.APPLICATION_JSON));

        assertThat(modelo.responder("reglas", CONVERSACION, HERRAMIENTAS, (n, a) -> Map.of()))
                .isEqualTo(GeminiModeloConversacional.RESPUESTA_BLOQUEADA);
    }

    @Test
    void alAgotarLasRondasPideUnaRespuestaSinHerramientas() {
        var servidor = new MockRestServiceServer[1];
        var modelo = modelo(servidor, 1);
        servidor[0].expect(requestTo(url("principal"))).andRespond(withSuccess(LLAMADA, MediaType.APPLICATION_JSON));
        servidor[0].expect(requestTo(url("principal")))
                .andExpect(jsonPath("$.toolConfig.functionCallingConfig.mode").value("NONE"))
                .andRespond(withSuccess(texto("Te cuento lo que encontré."), MediaType.APPLICATION_JSON));

        assertThat(modelo.responder("reglas", CONVERSACION, HERRAMIENTAS, (n, a) -> Map.of("piezas", List.of())))
                .isEqualTo("Te cuento lo que encontré.");
        servidor[0].verify();
    }

    @Test
    void sinLlaveElAsistenteNoEstaDisponible() {
        var props = new GeminiProperties(" ", "principal", List.of(), BASE, Duration.ofSeconds(5), 6);
        assertThat(new GeminiModeloConversacional(props, json, RestClient.builder(), reloj).disponible()).isFalse();
        assertThat(props.toString()).contains("llaveApi=(vacía)");
        assertThat(new GeminiProperties("secreto", "m", List.of(), BASE, Duration.ofSeconds(5), 6).toString())
                .doesNotContain("secreto").contains("llaveApi=(definida)");
    }
}
