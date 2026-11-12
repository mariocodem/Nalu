package co.nalu.api.publico;

import static co.nalu.api.error.Validacion.validar;

import co.nalu.api.UsuarioActual;
import co.nalu.negocio.asistente.AsistenteService;
import co.nalu.negocio.asistente.dto.AsistenteDtos.EstadoAsistente;
import co.nalu.negocio.asistente.dto.AsistenteDtos.RespuestaAsistente;
import co.nalu.negocio.asistente.dto.AsistenteDtos.SolicitudAsistente;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Asistente de compras con IA (ADR 005). Público: un visitante puede preguntar; si llega un
 * access token de cliente, el asistente también consulta sus pedidos.
 * Contrato: docs/arquitectura/api-asistente.md.
 */
@RestController
@RequestMapping("/api/v1/asistente")
public class AsistenteController {

    private final AsistenteService asistente;

    public AsistenteController(AsistenteService asistente) {
        this.asistente = asistente;
    }

    @GetMapping("/estado")
    EstadoAsistente estado() {
        return asistente.estado();
    }

    @PostMapping("/mensajes")
    RespuestaAsistente mensaje(@AuthenticationPrincipal Jwt jwt, @RequestBody SolicitudAsistente solicitud,
                               HttpServletRequest peticion) {
        validar(solicitud, SolicitudAsistente.ESQUEMA);
        var idCliente = esCliente(jwt) ? UsuarioActual.id(jwt) : null;
        var claveLimite = jwt != null ? "usuario:" + jwt.getSubject() : "ip:" + peticion.getRemoteAddr();
        return asistente.responder(solicitud, idCliente, claveLimite);
    }

    private static boolean esCliente(Jwt jwt) {
        if (jwt == null) {
            return false;
        }
        var roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("CLIENTE");
    }
}
