package co.nalu.api.publico;

import static co.nalu.api.error.Validacion.validar;

import co.nalu.api.UsuarioActual;
import co.nalu.negocio.carrito.CarritoService;
import co.nalu.negocio.carrito.dto.CarritoDtos;
import co.nalu.negocio.carrito.dto.CarritoDtos.AgregarItemRequest;
import co.nalu.negocio.carrito.dto.CarritoDtos.CambiarCantidadRequest;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RF08. Un visitante se identifica con el header {@value #CABECERA}; un cliente autenticado, con
 * su token. Contrato: docs/arquitectura/api-fase3.md §1.
 */
@RestController
@RequestMapping("/api/v1/carrito")
public class CarritoController {

    static final String CABECERA = "X-Cart-Token";

    private final CarritoService carritos;

    public CarritoController(CarritoService carritos) {
        this.carritos = carritos;
    }

    @GetMapping
    CarritoDtos.Carrito ver(@AuthenticationPrincipal Jwt jwt, @RequestHeader(name = CABECERA, required = false) String token) {
        return carritos.ver(idCliente(jwt), token);
    }

    @PostMapping("/items")
    CarritoDtos.Carrito agregar(@AuthenticationPrincipal Jwt jwt, @RequestHeader(name = CABECERA, required = false) String token,
                                @RequestBody AgregarItemRequest solicitud) {
        return carritos.agregar(idCliente(jwt), token, validar(solicitud, AgregarItemRequest.ESQUEMA));
    }

    @PatchMapping("/items/{id}")
    CarritoDtos.Carrito cambiar(@AuthenticationPrincipal Jwt jwt, @RequestHeader(name = CABECERA, required = false) String token,
                                @PathVariable long id, @RequestBody CambiarCantidadRequest solicitud) {
        return carritos.cambiarCantidad(idCliente(jwt), token, id, validar(solicitud, CambiarCantidadRequest.ESQUEMA).cantidad());
    }

    @DeleteMapping("/items/{id}")
    CarritoDtos.Carrito quitar(@AuthenticationPrincipal Jwt jwt, @RequestHeader(name = CABECERA, required = false) String token,
                               @PathVariable long id) {
        return carritos.quitar(idCliente(jwt), token, id);
    }

    /** Al iniciar sesión: pasa el carrito del visitante a la cuenta (rol CLIENTE). */
    @PostMapping("/fusionar")
    CarritoDtos.Carrito fusionar(@AuthenticationPrincipal Jwt jwt, @RequestHeader(name = CABECERA, required = false) String token) {
        return carritos.fusionar(UsuarioActual.id(jwt), token);
    }

    /** Solo los clientes tienen carrito de cuenta; un administrador compra como visitante. */
    private static Long idCliente(Jwt jwt) {
        return jwt != null && jwt.getClaimAsStringList("roles") != null
                && jwt.getClaimAsStringList("roles").equals(List.of("CLIENTE")) ? UsuarioActual.id(jwt) : null;
    }
}
