package co.nalu.negocio.pedidos.dto;

import am.ik.yavi.core.Validator;
import co.nalu.datos.entidad.EstadoPago;
import co.nalu.datos.entidad.EstadoPedido;
import co.nalu.negocio.comun.EsquemaValidacion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** RF14 (administración). Contrato: docs/arquitectura/api-fase5.md §1. */
public final class GestionPedidoDtos {

    private GestionPedidoDtos() {}

    public record FiltroPedidos(EstadoPedido estado, LocalDate desde, LocalDate hasta, String q) {}

    public record PedidoAdminResumen(String numero, OffsetDateTime fecha, String cliente, String correo,
                                     EstadoPedido estado, BigDecimal total, int piezas) {}

    public record PagoAdmin(String referencia, EstadoPago estado, String metodo, BigDecimal monto,
                            String referenciaExterna, OffsetDateTime fecha) {}

    public record ClienteAdmin(String nombre, String correo, String telefono) {}

    public record PedidoAdminDetalle(PedidoDtos.PedidoDetalle pedido, ClienteAdmin cliente, List<PagoAdmin> pagos,
                                     List<EstadoPedido> siguientes) {}

    public record CambioEstadoRequest(EstadoPedido estado, String nota) {
        public CambioEstadoRequest {
            nota = nota == null || nota.isBlank() ? null : nota.strip();
        }

        public static final Validator<CambioEstadoRequest> ESQUEMA = EsquemaValidacion.para(CambioEstadoRequest.class)
                ._object(CambioEstadoRequest::estado, "estado", c -> c.notNull())
                ._string(CambioEstadoRequest::nota, "nota", c -> c.lessThanOrEqual(255))
                .build();
    }

    public record EnvioRequest(String transportadora, String numeroGuia, BigDecimal costo) {
        public EnvioRequest {
            transportadora = transportadora == null ? null : transportadora.strip();
            numeroGuia = numeroGuia == null ? null : numeroGuia.strip();
        }

        public static final Validator<EnvioRequest> ESQUEMA = EsquemaValidacion.para(EnvioRequest.class)
                ._string(EnvioRequest::transportadora, "transportadora", c -> c.notBlank().lessThanOrEqual(60))
                ._string(EnvioRequest::numeroGuia, "numeroGuia", c -> c.notBlank().lessThanOrEqual(60))
                ._bigDecimal(EnvioRequest::costo, "costo", c -> c.greaterThanOrEqual(BigDecimal.ZERO)
                        .lessThan(new BigDecimal("10000000")))
                .build();
    }
}
