package co.nalu.negocio.pedidos;

import co.nalu.datos.entidad.DetallePedido;
import co.nalu.datos.entidad.EstadoPago;
import co.nalu.datos.entidad.EstadoPedido;
import co.nalu.datos.entidad.ImagenProducto;
import co.nalu.datos.entidad.Pago;
import co.nalu.datos.entidad.Pedido;
import co.nalu.datos.repositorio.PedidoRepository;
import co.nalu.negocio.clientes.dto.DireccionDto;
import co.nalu.negocio.comun.Pagina;
import co.nalu.negocio.comun.RecursoNoEncontradoException;
import co.nalu.negocio.pedidos.dto.PedidoDtos.CambioEstado;
import co.nalu.negocio.pedidos.dto.PedidoDtos.EnvioDto;
import co.nalu.negocio.pedidos.dto.PedidoDtos.LineaPedido;
import co.nalu.negocio.pedidos.dto.PedidoDtos.PedidoDetalle;
import co.nalu.negocio.pedidos.dto.PedidoDtos.PedidoResumen;
import java.util.Comparator;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF14 (vista del cliente): sus pedidos. Uno ajeno responde 404 (RNF02). */
@Service
public class PedidoClienteService {

    private final PedidoRepository pedidos;

    public PedidoClienteService(PedidoRepository pedidos) {
        this.pedidos = pedidos;
    }

    @Transactional(readOnly = true)
    public Pagina<PedidoResumen> listar(long idCliente, Pageable pagina) {
        return Pagina.de(pedidos.findByClienteIdOrderByFechaDesc(idCliente, pagina), p -> new PedidoResumen(
                p.getNumeroPedido(), p.getFecha(), p.getEstado(), p.getTotal(),
                p.getDetalles().stream().mapToInt(DetallePedido::getCantidad).sum()));
    }

    @Transactional(readOnly = true)
    public PedidoDetalle detalle(long idCliente, String numero) {
        return detalle(pedidos.findByNumeroPedidoAndClienteId(numero, idCliente)
                .orElseThrow(() -> new RecursoNoEncontradoException("El pedido no existe")));
    }

    /** Detalle completo; también lo usa el panel de administración. */
    public static PedidoDetalle detalle(Pedido p) {
        var lineas = p.getDetalles().stream().map(d -> {
            var producto = d.getVariante().getProducto();
            var imagen = producto.getImagenes().stream().filter(ImagenProducto::isPrincipal).findFirst()
                    .map(ImagenProducto::getUrl).orElse(null);
            return new LineaPedido(producto.getNombre(), d.getVariante().getTalla(), producto.getSlug(), imagen,
                    d.getCantidad(), d.getPrecioUnitario(), d.getDescuentoAplicado(),
                    d.getSubtotal().subtract(d.getDescuentoAplicado()));
        }).toList();
        var d = p.getDireccion();
        var direccion = new DireccionDto(d.getId(), d.getEtiqueta(), d.getDestinatario(), d.getDepartamento(),
                d.getCiudad(), d.getDireccion(), d.getCodigoPostal(), d.getTelefono());
        var historial = p.getHistorial().stream()
                .map(h -> new CambioEstado(h.getEstadoNuevo(), h.getFecha(), h.getNota())).toList();
        var ultimoPago = p.getPagos().stream().max(Comparator.comparing(Pago::getId)).map(Pago::getEstado).orElse(null);
        boolean puedePagar = p.getEstado() == EstadoPedido.PENDIENTE_PAGO && ultimoPago != EstadoPago.PENDIENTE
                && ultimoPago != EstadoPago.APROBADO;
        return new PedidoDetalle(p.getNumeroPedido(), p.getFecha(), p.getEstado(), lineas, p.getSubtotal(),
                p.getDescuento(), p.getCostoEnvio(), p.getTotal(),
                p.getPromocion() == null ? null : p.getPromocion().getCodigo(), direccion, historial, ultimoPago,
                puedePagar, p.isReservaActiva() ? p.getReservaExpiraEn() : null,
                p.getEnvio() == null ? null : new EnvioDto(
                        p.getEnvio().getTransportadora(), p.getEnvio().getNumeroGuia(), p.getEnvio().getFechaDespacho(),
                        p.getEnvio().getFechaEntrega()));
    }
}
