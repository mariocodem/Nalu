package co.nalu.integracion.gemini;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Conexión con la API de Gemini (ADR 005). La llave solo llega por variable de entorno.
 *
 * @param llaveApi {@code GEMINI_API_KEY}; vacía desactiva el asistente
 * @param modelo modelo principal ({@code GEMINI_MODEL})
 * @param modelosRespaldo modelos que se prueban, en orden, si el principal está saturado o no existe
 * @param tiempoEspera límite de cada llamada al proveedor
 * @param maxRondas rondas de herramientas por mensaje antes de pedir una respuesta sin herramientas
 */
@ConfigurationProperties("nalu.gemini")
public record GeminiProperties(
        String llaveApi,
        @DefaultValue("gemini-flash-latest") String modelo,
        @DefaultValue("gemini-flash-lite-latest") List<String> modelosRespaldo,
        @DefaultValue("https://generativelanguage.googleapis.com/v1beta") String urlBase,
        @DefaultValue("15s") Duration tiempoEspera,
        @DefaultValue("6") int maxRondas) {

    @Override
    public String toString() {
        return "GeminiProperties[modelo=" + modelo + ", modelosRespaldo=" + modelosRespaldo + ", urlBase=" + urlBase
                + ", llaveApi=" + (llaveApi == null || llaveApi.isBlank() ? "(vacía)" : "(definida)") + "]";
    }
}
