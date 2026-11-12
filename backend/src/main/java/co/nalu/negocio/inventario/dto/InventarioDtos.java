package co.nalu.negocio.inventario.dto;

import am.ik.yavi.core.Validator;
import co.nalu.datos.entidad.TipoMovimiento;
import co.nalu.negocio.comun.EsquemaValidacion;
import java.time.OffsetDateTime;

/** RF13. Contrato: docs/arquitectura/api-fase5.md §2. */
public final class InventarioDtos {

    private InventarioDtos() {}

    /**
     * {@code cantidad}: unidades positivas en ENTRADA y SALIDA; en AJUSTE, el cambio con signo
     * (p. ej. −1 por una pieza dañada).
     */
    public record MovimientoRequest(Long idVariante, TipoMovimiento tipo, Integer cantidad, String motivo) {
        public MovimientoRequest {
            motivo = motivo == null ? null : motivo.strip();
        }

        public static final Validator<MovimientoRequest> ESQUEMA = EsquemaValidacion.para(MovimientoRequest.class)
                ._long(MovimientoRequest::idVariante, "idVariante", c -> c.notNull())
                ._object(MovimientoRequest::tipo, "tipo", c -> c.notNull())
                ._integer(MovimientoRequest::cantidad, "cantidad", c -> c.notNull().greaterThanOrEqual(-100000)
                        .lessThanOrEqual(100000))
                ._string(MovimientoRequest::motivo, "motivo", c -> c.notBlank().lessThanOrEqual(160))
                .constraintOnTarget(m -> m.cantidad() == null || m.cantidad() != 0, "cantidad", "cantidad.cero",
                        "La cantidad no puede ser cero")
                .constraintOnTarget(m -> m.cantidad() == null || m.tipo() == null || m.tipo() == TipoMovimiento.AJUSTE
                        || m.cantidad() > 0, "cantidad", "cantidad.positiva",
                        "En entradas y salidas la cantidad debe ser positiva")
                .build();
    }

    public record Movimiento(Long id, OffsetDateTime fecha, Long idVariante, String sku, String producto, String talla,
                             TipoMovimiento tipo, int cantidad, String motivo, String responsable, String numeroPedido) {}

    public record Alerta(Long idVariante, Long idProducto, String sku, String producto, String talla, int stockActual,
                         int stockReservado, int disponible) {}
}
