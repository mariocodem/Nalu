package co.nalu.api;

import co.nalu.negocio.auditoria.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Registra cada operación de escritura del panel (RNF15): quién, cuándo, qué ruta y con qué
 * resultado. No guarda cuerpos de petición (pueden llevar datos personales).
 */
@Component
public class AuditoriaInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaInterceptor.class);
    private static final Set<String> ESCRITURA = Set.of("POST", "PUT", "PATCH", "DELETE");
    /** /api/v1/admin/{recurso}/{id}/… */
    private static final Pattern RUTA = Pattern.compile("^/api/v1/admin/([a-z-]+)(?:/([^/]+))?.*$");

    private final AuditoriaService auditoria;

    public AuditoriaInterceptor(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    @Override
    public void afterCompletion(HttpServletRequest solicitud, HttpServletResponse respuesta, Object handler, Exception ex) {
        if (!ESCRITURA.contains(solicitud.getMethod())) {
            return;
        }
        try {
            var ruta = solicitud.getRequestURI();
            var m = RUTA.matcher(ruta);
            Long idUsuario = SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwt
                    ? Long.valueOf(jwt.getToken().getSubject()) : null;
            auditoria.registrarAccion(idUsuario, solicitud.getMethod() + " " + ruta,
                    m.matches() ? m.group(1) : null, m.matches() ? m.group(2) : null, "HTTP " + respuesta.getStatus());
        } catch (RuntimeException e) {
            log.error("No se pudo registrar la auditoría de {} {}", solicitud.getMethod(), solicitud.getRequestURI(), e);
        }
    }
}
