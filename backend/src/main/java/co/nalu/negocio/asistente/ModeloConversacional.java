package co.nalu.negocio.asistente;

import java.util.List;
import java.util.Map;

/**
 * Puerto hacia el modelo de lenguaje que redacta las respuestas del asistente de compras (ADR 005).
 * Lo implementa {@code integracion.gemini}; el negocio no conoce el proveedor.
 *
 * <p>El adaptador resuelve un turno completo: envía las instrucciones y la conversación, ejecuta
 * con el {@link EjecutorHerramientas} cada herramienta que el modelo pida y devuelve el texto final.
 */
public interface ModeloConversacional {

    /** Indica si el proveedor está configurado (hay llave de API). */
    boolean disponible();

    /**
     * @param instrucciones reglas del asistente (instrucción de sistema)
     * @param conversacion mensajes en orden; el último es el del cliente
     * @param herramientas funciones que el modelo puede pedir
     * @param ejecutor ejecuta una herramienta y devuelve su resultado (se serializa como JSON)
     * @return texto de la respuesta para el cliente
     * @throws AsistenteNoDisponibleException si el proveedor no responde
     */
    String responder(String instrucciones, List<Mensaje> conversacion, List<Herramienta> herramientas,
                     EjecutorHerramientas ejecutor);

    enum Autor { CLIENTE, ASISTENTE }

    record Mensaje(Autor autor, String texto) {}

    /**
     * @param parametros esquema de los argumentos (subconjunto de OpenAPI: {@code type},
     *     {@code properties}, {@code required}, {@code items}, {@code description})
     */
    record Herramienta(String nombre, String descripcion, Map<String, Object> parametros) {}

    @FunctionalInterface
    interface EjecutorHerramientas {
        Object ejecutar(String nombre, Map<String, Object> argumentos);
    }
}
