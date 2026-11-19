package co.nalu.negocio.pagos.dto;

import co.nalu.datos.entidad.EstadoPago;
import co.nalu.datos.entidad.EstadoPedido;

/** Contrato: docs/arquitectura/api-fase3.md §3. */
public final class PagoDtos {

    private PagoDtos() {}

    /** Resultado de registrar la respuesta de la pasarela: el pedido y el estado del intento de pago. */
    public record RespuestaRegistrada(String numero, EstadoPedido estado, EstadoPago estadoPago) {}
}
