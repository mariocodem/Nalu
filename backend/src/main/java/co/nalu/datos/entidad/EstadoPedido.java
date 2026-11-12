package co.nalu.datos.entidad;

import java.util.EnumSet;
import java.util.Set;

/**
 * Máquina de estados del pedido (plan §6.6, RF14):
 * PENDIENTE_PAGO → PAGADO → EN_PREPARACION → ENVIADO → ENTREGADO, y CANCELADO desde
 * PENDIENTE_PAGO, PAGADO o EN_PREPARACION.
 */
public enum EstadoPedido {
    PENDIENTE_PAGO, PAGADO, EN_PREPARACION, ENVIADO, ENTREGADO, CANCELADO;

    public Set<EstadoPedido> siguientes() {
        return switch (this) {
            case PENDIENTE_PAGO -> EnumSet.of(PAGADO, CANCELADO);
            case PAGADO -> EnumSet.of(EN_PREPARACION, CANCELADO);
            case EN_PREPARACION -> EnumSet.of(ENVIADO, CANCELADO);
            case ENVIADO -> EnumSet.of(ENTREGADO);
            case ENTREGADO, CANCELADO -> EnumSet.noneOf(EstadoPedido.class);
        };
    }

    public boolean puedeTransicionarA(EstadoPedido destino) {
        return siguientes().contains(destino);
    }
}
