package co.nalu.api.cliente;

import static co.nalu.api.error.Validacion.validar;

import co.nalu.api.Paginacion;
import co.nalu.api.UsuarioActual;
import co.nalu.negocio.comun.Pagina;
import co.nalu.negocio.pagos.PagoService;
import co.nalu.negocio.pagos.PasarelaPagos.DatosPago;
import co.nalu.negocio.pedidos.CheckoutService;
import co.nalu.negocio.pedidos.PedidoClienteService;
import co.nalu.negocio.pedidos.dto.PedidoDtos.Cotizacion;
import co.nalu.negocio.pedidos.dto.PedidoDtos.CotizarRequest;
import co.nalu.negocio.pedidos.dto.PedidoDtos.CrearPedidoRequest;
import co.nalu.negocio.pedidos.dto.PedidoDtos.PedidoCreado;
import co.nalu.negocio.pedidos.dto.PedidoDtos.PedidoDetalle;
import co.nalu.negocio.pedidos.dto.PedidoDtos.PedidoResumen;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** RF09–RF12 y RF14 (cliente). Contrato: docs/arquitectura/api-fase3.md §2–§4. */
@RestController
@RequestMapping("/api/v1")
public class PedidoController {

    private final CheckoutService checkout;
    private final PedidoClienteService pedidos;
    private final PagoService pagos;

    public PedidoController(CheckoutService checkout, PedidoClienteService pedidos, PagoService pagos) {
        this.checkout = checkout;
        this.pedidos = pedidos;
        this.pagos = pagos;
    }

    @PostMapping("/checkout/cotizar")
    Cotizacion cotizar(@AuthenticationPrincipal Jwt jwt, @RequestBody(required = false) CotizarRequest solicitud) {
        return checkout.cotizar(UsuarioActual.id(jwt), solicitud == null ? null : solicitud.codigoCupon());
    }

    @PostMapping("/pedidos")
    @ResponseStatus(HttpStatus.CREATED)
    PedidoCreado crear(@AuthenticationPrincipal Jwt jwt, @RequestBody CrearPedidoRequest solicitud) {
        return checkout.crearPedido(UsuarioActual.id(jwt), validar(solicitud, CrearPedidoRequest.ESQUEMA));
    }

    @PostMapping("/pedidos/{numero}/reintentar-pago")
    DatosPago reintentarPago(@AuthenticationPrincipal Jwt jwt, @PathVariable String numero) {
        return pagos.reintentar(UsuarioActual.id(jwt), numero);
    }

    @GetMapping("/cuenta/pedidos")
    Pagina<PedidoResumen> misPedidos(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return pedidos.listar(UsuarioActual.id(jwt), Paginacion.de(page, size, null, Set.of(), Sort.unsorted()));
    }

    @GetMapping("/cuenta/pedidos/{numero}")
    PedidoDetalle miPedido(@AuthenticationPrincipal Jwt jwt, @PathVariable String numero) {
        return pedidos.detalle(UsuarioActual.id(jwt), numero);
    }
}
