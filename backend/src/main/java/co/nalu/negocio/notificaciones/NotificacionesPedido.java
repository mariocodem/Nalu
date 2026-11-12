package co.nalu.negocio.notificaciones;

import co.nalu.config.AppProperties;
import co.nalu.datos.repositorio.PedidoRepository;
import co.nalu.negocio.notificaciones.Notificador.Mensaje;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Correos al cliente (RF14, plan §6.4 paso 4). Se envían solo después del commit: si la
 * transacción se revierte, no sale ningún correo. Un fallo del correo no afecta a la venta.
 */
@Component
public class NotificacionesPedido {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesPedido.class);
    private static final Locale COLOMBIA = Locale.of("es", "CO");

    private final PedidoRepository pedidos;
    private final Notificador notificador;
    private final AppProperties app;

    public NotificacionesPedido(PedidoRepository pedidos, Notificador notificador, AppProperties app) {
        this.pedidos = pedidos;
        this.notificador = notificador;
        this.app = app;
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void alPagar(EventosPedido.PedidoPagado evento) {
        enviar(evento.numeroPedido(), "Recibimos tu pago · pedido %s",
                "Hola, %s:\n\nConfirmamos el pago de tu pedido %s por %s. Ya lo estamos preparando y te avisaremos cuando salga.\n\nPuedes seguirlo en %s\n\nGracias por elegir Nalu.");
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void alRechazarPago(EventosPedido.PagoRechazado evento) {
        enviar(evento.numeroPedido(), "Tu pago no se completó · pedido %s",
                "Hola, %s:\n\nEl pago de tu pedido %s por %s no se completó. Puedes intentarlo de nuevo desde %s\n\nSi necesitas ayuda, responde a este correo.");
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void alCancelar(EventosPedido.PedidoCancelado evento) {
        enviar(evento.numeroPedido(), "Pedido %s cancelado",
                "Hola, %s:\n\nTu pedido %s por %s fue cancelado. Puedes ver el detalle en %s");
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void alEnviar(EventosPedido.PedidoEnviado evento) {
        enviar(evento.numeroPedido(), "Tu pedido %s va en camino",
                "Hola, %s:\n\nTu pedido %s por %s ya salió. Encuentra la transportadora y el número de guía en %s");
    }

    private void enviar(String numero, String asunto, String plantilla) {
        try {
            var pedido = pedidos.findByNumeroPedido(numero).orElseThrow();
            var cliente = pedido.getCliente();
            var enlace = app.origenFrontend() + "/cuenta/pedidos/" + numero;
            notificador.enviar(new Mensaje(cliente.getCorreo(), asunto.formatted(numero),
                    plantilla.formatted(cliente.getNombre(), numero, pesos(pedido.getTotal()), enlace)));
        } catch (RuntimeException e) {
            log.error("No se pudo enviar la notificación del pedido {}", numero, e);
        }
    }

    private static String pesos(BigDecimal valor) {
        var formato = NumberFormat.getCurrencyInstance(COLOMBIA);
        formato.setMaximumFractionDigits(0);
        return formato.format(valor);
    }
}
