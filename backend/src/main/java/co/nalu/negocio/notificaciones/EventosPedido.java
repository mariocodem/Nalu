package co.nalu.negocio.notificaciones;

/** Eventos de dominio que generan notificaciones después del commit. */
public final class EventosPedido {

    private EventosPedido() {}

    public record PedidoPagado(String numeroPedido) {}

    public record PagoRechazado(String numeroPedido) {}

    public record PedidoCancelado(String numeroPedido) {}

    public record PedidoEnviado(String numeroPedido) {}
}
