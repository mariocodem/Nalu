package co.nalu.negocio.pedidos;

import co.nalu.config.VentasProperties;
import co.nalu.datos.entidad.EstadoPago;
import co.nalu.datos.entidad.EstadoPedido;
import co.nalu.datos.repositorio.PedidoRepository;
import co.nalu.negocio.inventario.InventarioService;
import co.nalu.negocio.notificaciones.EventosPedido;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tareas programadas de pedidos (ADR 003): liberar reservas vencidas (plan §6.5) y cancelar los
 * pedidos que no se pagaron en 24 horas. Cada pedido se bloquea antes de tocarlo, para no
 * chocar con un webhook que llegue a la vez.
 */
@Component
public class TareasPedidos {

    private static final Logger log = LoggerFactory.getLogger(TareasPedidos.class);

    private final PedidoRepository pedidos;
    private final InventarioService inventario;
    private final VentasProperties ventas;
    private final ApplicationEventPublisher publicador;
    private final Clock reloj;

    public TareasPedidos(PedidoRepository pedidos, InventarioService inventario, VentasProperties ventas,
                         ApplicationEventPublisher publicador, Clock reloj) {
        this.pedidos = pedidos;
        this.inventario = inventario;
        this.ventas = ventas;
        this.publicador = publicador;
        this.reloj = reloj;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    @Transactional
    public void liberarReservasVencidas() {
        var ahora = OffsetDateTime.now(reloj);
        for (var candidato : pedidos.findByReservaActivaTrueAndReservaExpiraEnBefore(ahora)) {
            var pedido = pedidos.bloquear(candidato.getId()).orElseThrow();
            if (pedido.isReservaActiva() && pedido.getEstado() == EstadoPedido.PENDIENTE_PAGO) {
                inventario.liberar(pedido);
                log.info("Reserva del pedido {} liberada por vencimiento", pedido.getNumeroPedido());
            }
        }
    }

    @Scheduled(fixedDelay = 15 * 60_000, initialDelay = 120_000)
    @Transactional
    public void cancelarPedidosSinPago() {
        var limite = OffsetDateTime.now(reloj).minus(ventas.cancelacionSinPago());
        for (var candidato : pedidos.findByEstadoAndFechaBefore(EstadoPedido.PENDIENTE_PAGO, limite)) {
            var pedido = pedidos.bloquear(candidato.getId()).orElseThrow();
            boolean pagoEnCurso = pedido.getPagos().stream()
                    .anyMatch(p -> p.getEstado() == EstadoPago.PENDIENTE || p.getEstado() == EstadoPago.APROBADO);
            if (pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO || pagoEnCurso) {
                continue;
            }
            if (pedido.isReservaActiva()) {
                inventario.liberar(pedido);
            }
            pedido.cambiarEstado(EstadoPedido.CANCELADO, null, "Cancelado automáticamente: sin pago en 24 horas");
            publicador.publishEvent(new EventosPedido.PedidoCancelado(pedido.getNumeroPedido()));
            log.info("Pedido {} cancelado por falta de pago", pedido.getNumeroPedido());
        }
    }
}
