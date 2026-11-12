package co.nalu.integracion.gemini;

import co.nalu.negocio.asistente.AsistenteNoDisponibleException;
import co.nalu.negocio.asistente.ModeloConversacional;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Adaptador de la API de Gemini ({@code models/{modelo}:generateContent}) con llamadas a funciones.
 *
 * <ul>
 *   <li>El turno del modelo se devuelve tal cual llegó: los modelos actuales adjuntan una firma de
 *       razonamiento ({@code thoughtSignature}) a cada {@code functionCall} y exigen recibirla de
 *       vuelta. Cada {@code functionResponse} lleva el {@code id} de su llamada.
 *   <li>Si el modelo principal está saturado (429/5xx), no existe (404) o no responde a tiempo, se
 *       prueban los modelos de respaldo; solo antes de la primera herramienta, porque las firmas
 *       valen únicamente para el modelo que las produjo. A mitad de turno se reintenta el mismo.
 *   <li>Un modelo que falla por saturación o tiempo de espera queda en pausa {@link #PAUSA} y los
 *       turnos siguientes van directo al respaldo; al vencer la pausa se vuelve a probar.
 *   <li>Tras {@code maxRondas} rondas de herramientas se pide una respuesta sin herramientas.
 *   <li>La llave viaja en la cabecera {@code x-goog-api-key} y nunca se registra.
 * </ul>
 */
@Component
public class GeminiModeloConversacional implements ModeloConversacional {

    private static final Logger log = LoggerFactory.getLogger(GeminiModeloConversacional.class);
    static final Duration PAUSA = Duration.ofMinutes(2);
    private static final Set<Integer> REINTENTABLES = Set.of(404, 408, 429, 500, 502, 503, 504);
    private static final Set<String> FINALES_BLOQUEADOS = Set.of("SAFETY", "PROHIBITED_CONTENT", "BLOCKLIST", "SPII",
            "RECITATION", "IMAGE_SAFETY");
    static final String RESPUESTA_BLOQUEADA = "Prefiero no responder a eso. ¿Te ayudo a encontrar una pieza o a "
            + "resolver una duda sobre tu compra?";
    static final String RESPUESTA_VACIA = "No logré armar una respuesta. ¿Me lo puedes preguntar de otra forma?";
    private static final String NO_RESPONDE = "El asistente no está disponible en este momento. Inténtalo de nuevo "
            + "en unos minutos.";

    private final GeminiProperties props;
    private final JsonMapper json;
    private final RestClient http;
    private final Clock reloj;
    private final Map<String, Instant> pausados = new ConcurrentHashMap<>();

    @Autowired
    public GeminiModeloConversacional(GeminiProperties props, JsonMapper json, Clock reloj) {
        this(props, json, RestClient.builder().requestFactory(fabrica(props.tiempoEspera())), reloj);
    }

    GeminiModeloConversacional(GeminiProperties props, JsonMapper json, RestClient.Builder builder, Clock reloj) {
        this.props = props;
        this.json = json;
        this.http = builder.baseUrl(props.urlBase()).build();
        this.reloj = reloj;
    }

    private static JdkClientHttpRequestFactory fabrica(Duration tiempoEspera) {
        var fabrica = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        fabrica.setReadTimeout(tiempoEspera);
        return fabrica;
    }

    @Override
    public boolean disponible() {
        return props.llaveApi() != null && !props.llaveApi().isBlank();
    }

    @Override
    public String responder(String instrucciones, List<Mensaje> conversacion, List<Herramienta> herramientas,
                            EjecutorHerramientas ejecutor) {
        var contenidos = json.createArrayNode();
        for (var m : conversacion) {
            contenidos.add(contenido(m.autor() == Autor.CLIENTE ? "user" : "model", m.texto()));
        }
        String modeloDelTurno = null;
        for (int ronda = 0; ronda < props.maxRondas(); ronda++) {
            var resultado = generar(cuerpo(instrucciones, contenidos, herramientas, true), modeloDelTurno);
            modeloDelTurno = resultado.modelo();
            var contenido = resultado.respuesta().path("candidates").path(0).path("content");
            var llamadas = new ArrayList<JsonNode>();
            for (var parte : contenido.path("parts")) {
                if (parte.has("functionCall")) {
                    llamadas.add(parte.get("functionCall"));
                }
            }
            if (llamadas.isEmpty()) {
                return texto(resultado.respuesta());
            }
            contenidos.add(contenido.deepCopy());
            var respuestas = json.createArrayNode();
            for (var llamada : llamadas) {
                respuestas.add(respuestaDeFuncion(llamada, ejecutor));
            }
            contenidos.add(json.createObjectNode().put("role", "user").set("parts", respuestas));
        }
        log.info("El asistente alcanzó {} rondas de herramientas; se pide una respuesta sin herramientas", props.maxRondas());
        return texto(generar(cuerpo(instrucciones, contenidos, herramientas, false), modeloDelTurno).respuesta());
    }

    private ObjectNode respuestaDeFuncion(JsonNode llamada, EjecutorHerramientas ejecutor) {
        var nombre = llamada.path("name").asString();
        @SuppressWarnings("unchecked")
        Map<String, Object> argumentos = llamada.has("args") ? json.convertValue(llamada.get("args"), Map.class) : Map.of();
        Object resultado;
        try {
            resultado = ejecutor.ejecutar(nombre, argumentos);
        } catch (RuntimeException e) {
            log.warn("La herramienta {} falló dentro del turno del asistente", nombre, e);
            resultado = Map.of("error", "No se pudo completar la consulta");
        }
        JsonNode nodo = json.valueToTree(resultado);
        var respuesta = nodo != null && nodo.isObject() ? (ObjectNode) nodo : json.createObjectNode().set("resultado", nodo);
        var funcion = json.createObjectNode().put("name", nombre);
        if (llamada.hasNonNull("id")) {
            funcion.put("id", llamada.get("id").asString());
        }
        funcion.set("response", respuesta);
        return json.createObjectNode().set("functionResponse", funcion);
    }

    private ObjectNode cuerpo(String instrucciones, ArrayNode contenidos, List<Herramienta> herramientas, boolean conHerramientas) {
        var cuerpo = json.createObjectNode();
        cuerpo.set("systemInstruction", json.createObjectNode().set("parts",
                json.createArrayNode().add(json.createObjectNode().put("text", instrucciones))));
        cuerpo.set("contents", contenidos);
        if (!herramientas.isEmpty()) {
            var declaraciones = json.createArrayNode();
            for (var h : herramientas) {
                declaraciones.add(json.createObjectNode().put("name", h.nombre()).put("description", h.descripcion())
                        .set("parameters", json.valueToTree(h.parametros())));
            }
            cuerpo.set("tools", json.createArrayNode().add(json.createObjectNode().set("functionDeclarations", declaraciones)));
            if (!conHerramientas) {
                cuerpo.set("toolConfig", json.createObjectNode().set("functionCallingConfig",
                        json.createObjectNode().put("mode", "NONE")));
            }
        }
        return cuerpo;
    }

    private ObjectNode contenido(String rol, String texto) {
        return json.createObjectNode().put("role", rol)
                .set("parts", json.createArrayNode().add(json.createObjectNode().put("text", texto)));
    }

    private String texto(JsonNode respuesta) {
        var candidato = respuesta.path("candidates").path(0);
        var bloqueo = respuesta.path("promptFeedback").path("blockReason");
        if (!bloqueo.isMissingNode() || FINALES_BLOQUEADOS.contains(candidato.path("finishReason").asString(""))) {
            return RESPUESTA_BLOQUEADA;
        }
        var texto = new StringBuilder();
        for (var parte : candidato.path("content").path("parts")) {
            if (parte.has("text") && !parte.path("thought").asBoolean(false)) {
                texto.append(parte.get("text").asString());
            }
        }
        return texto.isEmpty() ? RESPUESTA_VACIA : texto.toString().strip();
    }

    private record Resultado(String modelo, JsonNode respuesta) {}

    private record Http(int estado, String cuerpo) {}

    /**
     * @param modeloFijo modelo que ya respondió en este turno ({@code null} en la primera llamada)
     */
    private Resultado generar(ObjectNode cuerpo, String modeloFijo) {
        var candidatos = new LinkedHashSet<String>();
        int intentos = 1;
        if (modeloFijo != null) {
            candidatos.add(modeloFijo);
            intentos = 2;
        } else {
            var todos = new LinkedHashSet<String>();
            todos.add(props.modelo());
            todos.addAll(props.modelosRespaldo());
            var ahora = reloj.instant();
            todos.stream().filter(m -> !pausados.getOrDefault(m, Instant.MIN).isAfter(ahora)).forEach(candidatos::add);
            if (candidatos.isEmpty()) {
                candidatos.addAll(todos); // todos en pausa: mejor intentar que rendirse
            }
        }
        var solicitud = json.writeValueAsString(cuerpo);
        for (var modelo : candidatos) {
            for (int intento = 1; intento <= intentos; intento++) {
                Http http;
                try {
                    http = llamar(modelo, solicitud);
                } catch (ResourceAccessException e) {
                    log.warn("Gemini ({}) no respondió a tiempo: {}", modelo, e.getMessage());
                    pausar(modelo);
                    continue;
                }
                if (http.estado() / 100 == 2) {
                    pausados.remove(modelo);
                    var respuesta = json.readTree(http.cuerpo());
                    registrarUso(modelo, respuesta);
                    return new Resultado(modelo, respuesta);
                }
                var mensaje = mensajeDeError(http.cuerpo());
                if (!REINTENTABLES.contains(http.estado())) {
                    log.error("Gemini ({}) rechazó la solicitud: HTTP {} {}", modelo, http.estado(), mensaje);
                    throw new AsistenteNoDisponibleException(NO_RESPONDE);
                }
                log.warn("Gemini ({}) no está disponible: HTTP {} {}", modelo, http.estado(), mensaje);
                pausar(modelo);
                if (intento < intentos) {
                    esperar(Duration.ofMillis(800));
                }
            }
        }
        throw new AsistenteNoDisponibleException(NO_RESPONDE);
    }

    private void pausar(String modelo) {
        pausados.put(modelo, reloj.instant().plus(PAUSA));
    }

    private Http llamar(String modelo, String solicitud) {
        return http.post().uri("/models/{modelo}:generateContent", modelo)
                .header("x-goog-api-key", props.llaveApi())
                .contentType(MediaType.APPLICATION_JSON)
                .body(solicitud)
                .exchange((peticion, respuesta) -> {
                    try (var cuerpo = respuesta.getBody()) {
                        return new Http(respuesta.getStatusCode().value(),
                                new String(cuerpo.readAllBytes(), StandardCharsets.UTF_8));
                    }
                });
    }

    private String mensajeDeError(String cuerpo) {
        try {
            return json.readTree(cuerpo).path("error").path("message").asString("");
        } catch (RuntimeException e) {
            return "";
        }
    }

    private void registrarUso(String modelo, JsonNode respuesta) {
        if (log.isDebugEnabled()) {
            var uso = respuesta.path("usageMetadata");
            log.debug("Gemini ({}): {} tokens de entrada, {} de salida, {} de razonamiento", modelo,
                    uso.path("promptTokenCount").asInt(0), uso.path("candidatesTokenCount").asInt(0),
                    uso.path("thoughtsTokenCount").asInt(0));
        }
    }

    private static void esperar(Duration duracion) {
        try {
            Thread.sleep(duracion);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AsistenteNoDisponibleException(NO_RESPONDE);
        }
    }
}
