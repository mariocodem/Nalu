package co.nalu.negocio.inventario;

import co.nalu.datos.entidad.DetallePedido;
import co.nalu.datos.entidad.MovimientoInventario;
import co.nalu.datos.entidad.Pedido;
import co.nalu.datos.entidad.TipoMovimiento;
import co.nalu.datos.entidad.VarianteProducto;
import co.nalu.datos.repositorio.MovimientoInventarioRepository;
import co.nalu.datos.repositorio.VarianteProductoRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Único punto que cambia existencias por ventas (RF09, RF13). Siempre bloquea las filas de las
 * variantes en orden de id antes de tocarlas. Se ejecuta dentro de la transacción de quien lo
 * llama ({@link Propagation#MANDATORY}).
 */
@Service
public class InventarioService {

    private final VarianteProductoRepository variantes;
    private final MovimientoInventarioRepository movimientos;

    public InventarioService(VarianteProductoRepository variantes, MovimientoInventarioRepository movimientos) {
        this.variantes = variantes;
        this.movimientos = movimientos;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Map<Long, VarianteProducto> bloquear(Collection<Long> ids) {
        return variantes.findByIdInOrderByIdAsc(ids).stream()
                .collect(Collectors.toMap(VarianteProducto::getId, Function.identity()));
    }

    /** Reserva lo del pedido; el llamador ya comprobó la disponibilidad con las filas bloqueadas. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reservar(Pedido pedido) {
        var bloqueadas = bloquear(ids(pedido.getDetalles()));
        for (var d : pedido.getDetalles()) {
            bloqueadas.get(d.getVariante().getId()).reservar(d.getCantidad());
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void liberar(Pedido pedido) {
        var bloqueadas = bloquear(ids(pedido.getDetalles()));
        for (var d : pedido.getDetalles()) {
            bloqueadas.get(d.getVariante().getId()).liberar(d.getCantidad());
        }
        pedido.desactivarReserva();
    }

    /**
     * Pago aprobado (plan §6.4): las unidades reservadas salen del inventario con un movimiento
     * {@code SALIDA} por línea. Si la reserva ya había expirado, intenta tomarlas de lo
     * disponible; devuelve {@code false} si no alcanza (el pedido queda para revisión manual).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean confirmarSalida(Pedido pedido) {
        var bloqueadas = bloquear(ids(pedido.getDetalles()));
        if (!pedido.isReservaActiva()) {
            boolean alcanza = pedido.getDetalles().stream()
                    .allMatch(d -> bloqueadas.get(d.getVariante().getId()).disponible() >= d.getCantidad());
            if (!alcanza) {
                return false;
            }
            pedido.getDetalles().forEach(d -> bloqueadas.get(d.getVariante().getId()).reservar(d.getCantidad()));
        }
        for (var d : pedido.getDetalles()) {
            var variante = bloqueadas.get(d.getVariante().getId());
            variante.confirmarSalida(d.getCantidad());
            movimientos.save(MovimientoInventario.porPedido(variante, pedido, TipoMovimiento.SALIDA, -d.getCantidad(),
                    "Venta del pedido " + pedido.getNumeroPedido()));
        }
        pedido.desactivarReserva();
        return true;
    }

    /** Cancelación de un pedido ya pagado: las unidades vuelven al inventario (plan §6.6). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reintegrar(Pedido pedido) {
        var bloqueadas = bloquear(ids(pedido.getDetalles()));
        for (var d : pedido.getDetalles()) {
            var variante = bloqueadas.get(d.getVariante().getId());
            variante.ajustar(d.getCantidad());
            movimientos.save(MovimientoInventario.porPedido(variante, pedido, TipoMovimiento.ENTRADA, d.getCantidad(),
                    "Reintegro por cancelación del pedido " + pedido.getNumeroPedido()));
        }
    }

    private static List<Long> ids(List<DetallePedido> detalles) {
        return detalles.stream().map(d -> d.getVariante().getId()).distinct().toList();
    }
}
