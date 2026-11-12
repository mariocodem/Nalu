package co.nalu.negocio.asistente;

import co.nalu.config.AsistenteProperties;
import co.nalu.negocio.comun.DemasiadosIntentosException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Ventana deslizante en memoria por persona (cuenta o IP): controla el abuso y el gasto del
 * proveedor. Es local a cada instancia del backend; con varias instancias habría que moverla a un
 * almacén compartido.
 */
@Component
public class LimitadorAsistente {

    private static final int MAX_CLAVES = 10_000;

    private final AsistenteProperties props;
    private final Clock reloj;
    private final Map<String, Deque<Instant>> envios = new ConcurrentHashMap<>();

    public LimitadorAsistente(AsistenteProperties props, Clock reloj) {
        this.props = props;
        this.reloj = reloj;
    }

    /** @throws DemasiadosIntentosException si la persona ya agotó los mensajes de la ventana */
    public void registrar(String clave) {
        var ahora = reloj.instant();
        var desde = ahora.minus(props.ventana());
        var cola = envios.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (cola) {
            descartarAntiguos(cola, desde);
            if (cola.size() >= props.mensajesPorVentana()) {
                throw new DemasiadosIntentosException(
                        "Enviaste muchos mensajes seguidos. Espera unos minutos y vuelve a escribir.");
            }
            cola.addLast(ahora);
        }
        if (envios.size() > MAX_CLAVES) {
            envios.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    descartarAntiguos(e.getValue(), desde);
                    return e.getValue().isEmpty();
                }
            });
        }
    }

    private static void descartarAntiguos(Deque<Instant> cola, Instant desde) {
        while (!cola.isEmpty() && cola.peekFirst().isBefore(desde)) {
            cola.pollFirst();
        }
    }
}
