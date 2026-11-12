package co.nalu.api.publico;

import static co.nalu.api.error.Validacion.validar;

import co.nalu.api.error.OrigenNoPermitidoException;
import co.nalu.config.AppProperties;
import co.nalu.negocio.cuentas.AutenticacionService;
import co.nalu.negocio.cuentas.SesionEmitida;
import co.nalu.negocio.cuentas.dto.LoginRequest;
import co.nalu.negocio.cuentas.dto.RegistroRequest;
import co.nalu.negocio.cuentas.dto.SesionDto;
import co.nalu.negocio.cuentas.dto.UsuarioSesionDto;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de sesión. Contrato: {@code docs/arquitectura/api-autenticacion.md}. */
@RestController
@RequestMapping(AutenticacionController.RUTA)
public class AutenticacionController {

    static final String RUTA = "/api/v1/auth";
    static final String COOKIE_REFRESH = "nalu_rt";

    private final AutenticacionService autenticacion;
    private final AppProperties app;

    public AutenticacionController(AutenticacionService autenticacion, AppProperties app) {
        this.autenticacion = autenticacion;
        this.app = app;
    }

    @PostMapping("/login")
    ResponseEntity<SesionDto> login(@RequestBody LoginRequest solicitud, HttpServletRequest http) {
        validar(solicitud, LoginRequest.ESQUEMA);
        return conCookie(autenticacion.iniciarSesion(solicitud, http.getRemoteAddr(), userAgent(http)));
    }

    @PostMapping("/registro")
    ResponseEntity<SesionDto> registro(@RequestBody RegistroRequest solicitud, HttpServletRequest http) {
        validar(solicitud, RegistroRequest.ESQUEMA);
        var emitida = autenticacion.registrar(solicitud, http.getRemoteAddr(), userAgent(http));
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie(emitida.refreshToken(), emitida.vidaRefresh()).toString())
                .body(emitida.sesion());
    }

    @PostMapping("/refresh")
    ResponseEntity<SesionDto> refresh(@CookieValue(name = COOKIE_REFRESH, required = false) String refreshToken,
                                      @RequestHeader(name = HttpHeaders.ORIGIN, required = false) String origen,
                                      HttpServletRequest http) {
        verificarOrigen(origen);
        return conCookie(autenticacion.refrescar(refreshToken, http.getRemoteAddr(), userAgent(http)));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@CookieValue(name = COOKIE_REFRESH, required = false) String refreshToken,
                                @RequestHeader(name = HttpHeaders.ORIGIN, required = false) String origen) {
        verificarOrigen(origen);
        autenticacion.cerrarSesion(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .build();
    }

    @GetMapping("/me")
    UsuarioSesionDto me(@AuthenticationPrincipal Jwt jwt) {
        return autenticacion.usuarioActual(Long.parseLong(jwt.getSubject()));
    }

    /** Con cookie se exige que la petición venga del frontend: defensa adicional a SameSite=Strict. */
    private void verificarOrigen(String origen) {
        if (origen == null || !origen.equals(app.origenFrontend())) {
            throw new OrigenNoPermitidoException();
        }
    }

    private ResponseEntity<SesionDto> conCookie(SesionEmitida emitida) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(emitida.refreshToken(), emitida.vidaRefresh()).toString())
                .body(emitida.sesion());
    }

    private ResponseCookie cookie(String valor, Duration vida) {
        return ResponseCookie.from(COOKIE_REFRESH, valor)
                .httpOnly(true)
                .secure(app.cookieSegura())
                .sameSite("Strict")
                .path(RUTA)
                .maxAge(vida)
                .build();
    }

    private static String userAgent(HttpServletRequest http) {
        return http.getHeader(HttpHeaders.USER_AGENT);
    }
}
