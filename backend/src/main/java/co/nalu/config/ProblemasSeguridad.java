package co.nalu.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Respuestas 401 y 403 de Spring Security en {@code application/problem+json} y en español, con
 * el mismo formato que {@code api.error.ManejadorErrores}. El estado y el header
 * {@code WWW-Authenticate} los fijan los manejadores estándar de Bearer token.
 */
@Component
class ProblemasSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final BearerTokenAuthenticationEntryPoint entradaBearer = new BearerTokenAuthenticationEntryPoint();
    private final BearerTokenAccessDeniedHandler denegadoBearer = new BearerTokenAccessDeniedHandler();
    private final JsonMapper json;

    ProblemasSeguridad(JsonMapper json) {
        this.json = json;
    }

    @Override
    public void commence(HttpServletRequest solicitud, HttpServletResponse respuesta, AuthenticationException ex)
            throws IOException {
        entradaBearer.commence(solicitud, respuesta, ex);
        var detalle = ex instanceof InvalidBearerTokenException
                ? "El token de acceso no es válido o ha expirado."
                : "Debes iniciar sesión para acceder a este recurso.";
        escribir(respuesta, "urn:nalu:error:no-autenticado", "No autenticado", detalle, solicitud);
    }

    @Override
    public void handle(HttpServletRequest solicitud, HttpServletResponse respuesta, AccessDeniedException ex)
            throws IOException {
        denegadoBearer.handle(solicitud, respuesta, ex);
        escribir(respuesta, "urn:nalu:error:acceso-denegado", "Acceso denegado",
                "No tienes permiso para acceder a este recurso.", solicitud);
    }

    private void escribir(HttpServletResponse respuesta, String tipo, String titulo, String detalle,
                          HttpServletRequest solicitud) throws IOException {
        var problema = new LinkedHashMap<String, Object>();
        problema.put("type", tipo);
        problema.put("title", titulo);
        problema.put("status", respuesta.getStatus());
        problema.put("detail", detalle);
        problema.put("instance", solicitud.getRequestURI());
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        json.writeValue(respuesta.getOutputStream(), problema);
    }
}
