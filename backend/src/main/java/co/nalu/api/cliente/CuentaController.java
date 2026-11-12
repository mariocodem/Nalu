package co.nalu.api.cliente;

import static co.nalu.api.error.Validacion.validar;

import co.nalu.api.UsuarioActual;
import co.nalu.negocio.clientes.ClienteService;
import co.nalu.negocio.clientes.dto.DireccionDto;
import co.nalu.negocio.clientes.dto.DireccionRequest;
import co.nalu.negocio.clientes.dto.PerfilDto;
import co.nalu.negocio.clientes.dto.PerfilRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** RF02: perfil y direcciones del cliente autenticado (rol CLIENTE, SeguridadConfig). */
@RestController
@RequestMapping("/api/v1/cuenta")
public class CuentaController {

    private final ClienteService clientes;

    public CuentaController(ClienteService clientes) {
        this.clientes = clientes;
    }

    @GetMapping("/perfil")
    PerfilDto perfil(@AuthenticationPrincipal Jwt jwt) {
        return clientes.perfil(UsuarioActual.id(jwt));
    }

    @PutMapping("/perfil")
    PerfilDto actualizarPerfil(@AuthenticationPrincipal Jwt jwt, @RequestBody PerfilRequest solicitud) {
        return clientes.actualizarPerfil(UsuarioActual.id(jwt), validar(solicitud, PerfilRequest.ESQUEMA));
    }

    @GetMapping("/direcciones")
    List<DireccionDto> direcciones(@AuthenticationPrincipal Jwt jwt) {
        return clientes.direcciones(UsuarioActual.id(jwt));
    }

    @PostMapping("/direcciones")
    @ResponseStatus(HttpStatus.CREATED)
    DireccionDto crearDireccion(@AuthenticationPrincipal Jwt jwt, @RequestBody DireccionRequest solicitud) {
        return clientes.crearDireccion(UsuarioActual.id(jwt), validar(solicitud, DireccionRequest.ESQUEMA));
    }

    @PutMapping("/direcciones/{id}")
    DireccionDto actualizarDireccion(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                     @RequestBody DireccionRequest solicitud) {
        return clientes.actualizarDireccion(UsuarioActual.id(jwt), id, validar(solicitud, DireccionRequest.ESQUEMA));
    }

    @DeleteMapping("/direcciones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void eliminarDireccion(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        clientes.eliminarDireccion(UsuarioActual.id(jwt), id);
    }
}
