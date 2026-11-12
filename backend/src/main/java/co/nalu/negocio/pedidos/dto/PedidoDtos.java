package co.nalu.negocio.pedidos.dto;

import am.ik.yavi.core.Validator;
import co.nalu.datos.entidad.EstadoPago;
import co.nalu.datos.entidad.EstadoPedido;
import co.nalu.negocio.clientes.dto.DireccionDto;
import co.nalu.negocio.comun.EsquemaValidacion;
import co.nalu.negocio.pagos.PasarelaPagos.DatosPago;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Contrato: docs/arquitectura/api-fase3.md. */
public final class PedidoDtos {

    private PedidoDtos() {}

    public record CotizarRequest(String codigoCupon) {}

    public record CrearPedidoRequest(Long idDireccion, String codigoCupon) {
        public static final Validator<CrearPedidoRequest> ESQUEMA = EsquemaValidacion.para(CrearPedidoRequest.class)
                ._long(CrearPedidoRequest::idDireccion, "idDireccion", c -> c.notNull().message("Elige una dirección de envío"))
                ._string(CrearPedidoRequest::codigoCupon, "codigoCupon", c -> c.lessThanOrEqual(30))
                .build();
    }

    public record LineaCotizada(Long idVariante, String nombre, String talla, int cantidad, BigDecimal precioUnitario,
                                BigDecimal subtotal, boolean alcanza) {}

    public record Cupon(String codigo, boolean aplicado, String mensaje) {}

    public record Cotizacion(List<LineaCotizada> lineas, BigDecimal subtotal, BigDecimal descuento,
                             BigDecimal costoEnvio, BigDecimal total, Cupon cupon, List<String> faltantes) {}

    public record PedidoCreado(String numero, EstadoPedido estado, BigDecimal total, OffsetDateTime reservaExpiraEn,
                               DatosPago pago) {}

    public record PedidoResumen(String numero, OffsetDateTime fecha, EstadoPedido estado, BigDecimal total, int piezas) {}

    public record LineaPedido(String nombre, String talla, String slug, String imagen, int cantidad,
                              BigDecimal precioUnitario, BigDecimal descuento, BigDecimal subtotal) {}

    public record CambioEstado(EstadoPedido estado, OffsetDateTime fecha, String nota) {}

    public record PedidoDetalle(String numero, OffsetDateTime fecha, EstadoPedido estado, List<LineaPedido> lineas,
                                BigDecimal subtotal, BigDecimal descuento, BigDecimal costoEnvio, BigDecimal total,
                                String cupon, DireccionDto direccion, List<CambioEstado> historial,
                                EstadoPago estadoPago, boolean puedePagar, OffsetDateTime reservaExpiraEn,
                                EnvioDto envio) {}

    public record EnvioDto(String transportadora, String numeroGuia, OffsetDateTime fechaDespacho,
                           OffsetDateTime fechaEntrega) {}
}
