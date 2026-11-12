package co.nalu.api.admin;

import static co.nalu.api.error.Validacion.validar;

import co.nalu.api.Paginacion;
import co.nalu.api.UsuarioActual;
import co.nalu.negocio.comun.Pagina;
import co.nalu.negocio.pedidos.GestionPedidoService;
import co.nalu.negocio.pedidos.dto.GestionPedidoDtos.CambioEstadoRequest;
import co.nalu.negocio.pedidos.dto.GestionPedidoDtos.EnvioRequest;
import co.nalu.negocio.pedidos.dto.GestionPedidoDtos.FiltroPedidos;
import co.nalu.negocio.pedidos.dto.GestionPedidoDtos.PedidoAdminDetalle;
import co.nalu.negocio.pedidos.dto.GestionPedidoDtos.PedidoAdminResumen;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** RF14 (administración). Contrato: docs/arquitectura/api-fase5.md §1. */
@RestController
@RequestMapping("/api/v1/admin/pedidos")
public class PedidoAdminController {

    private final GestionPedidoService pedidos;

    public PedidoAdminController(GestionPedidoService pedidos) {
        this.pedidos = pedidos;
    }

    @GetMapping
    Pagina<PedidoAdminResumen> listar(FiltroPedidos filtro,
                                      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return pedidos.listar(filtro,
                Paginacion.de(page, size, null, Set.of(), Sort.by(Sort.Direction.DESC, "fecha")));
    }

    @GetMapping("/{numero}")
    PedidoAdminDetalle detalle(@PathVariable String numero) {
        return pedidos.detalle(numero);
    }

    @PatchMapping("/{numero}/estado")
    PedidoAdminDetalle cambiarEstado(@PathVariable String numero, @RequestBody CambioEstadoRequest solicitud,
                                     @AuthenticationPrincipal Jwt jwt) {
        return pedidos.cambiarEstado(numero, validar(solicitud, CambioEstadoRequest.ESQUEMA), UsuarioActual.id(jwt));
    }

    @PostMapping("/{numero}/envio")
    PedidoAdminDetalle registrarEnvio(@PathVariable String numero, @RequestBody EnvioRequest solicitud,
                                      @AuthenticationPrincipal Jwt jwt) {
        return pedidos.registrarEnvio(numero, validar(solicitud, EnvioRequest.ESQUEMA), UsuarioActual.id(jwt));
    }
}
