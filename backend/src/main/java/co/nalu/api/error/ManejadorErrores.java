package co.nalu.api.error;

import co.nalu.negocio.asistente.AsistenteNoDisponibleException;
import co.nalu.negocio.comun.ConflictoException;
import co.nalu.negocio.comun.DemasiadosIntentosException;
import co.nalu.negocio.comun.ExistenciasInsuficientesException;
import co.nalu.negocio.pagos.FirmaInvalidaException;
import co.nalu.negocio.comun.NoAutenticadoException;
import co.nalu.negocio.comun.RecursoNoEncontradoException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Convierte las excepciones en respuestas {@code application/problem+json} (RFC 9457) con textos en
 * español (RNF08).
 *
 * <ul>
 *   <li>Errores propios de Nalu: los métodos de esta clase.
 *   <li>Errores de Spring MVC (JSON ilegible, método no permitido, ruta inexistente…): la clase
 *       base, con títulos y detalles traducidos en {@code messages.properties}.
 *   <li>Errores de seguridad: se relanzan para que Spring Security responda 401/403.
 *   <li>Cualquier otro error: 500 sin exponer detalles internos.
 * </ul>
 *
 * <p>Los tipos de error usan URN ({@code urn:nalu:error:…}) porque el dominio definitivo aún no está
 * decidido (CLAUDE.md §12).
 */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    static final URI TIPO_VALIDACION = URI.create("urn:nalu:error:validacion");
    static final URI TIPO_NO_ENCONTRADO = URI.create("urn:nalu:error:no-encontrado");
    static final URI TIPO_CONFLICTO = URI.create("urn:nalu:error:conflicto");
    static final URI TIPO_INTERNO = URI.create("urn:nalu:error:interno");
    static final URI TIPO_NO_AUTENTICADO = URI.create("urn:nalu:error:no-autenticado");
    static final URI TIPO_EXISTENCIAS = URI.create("urn:nalu:error:existencias-insuficientes");
    static final URI TIPO_ORIGEN = URI.create("urn:nalu:error:origen-no-permitido");
    static final URI TIPO_DEMASIADOS_INTENTOS = URI.create("urn:nalu:error:demasiados-intentos");
    static final URI TIPO_ASISTENTE_NO_DISPONIBLE = URI.create("urn:nalu:error:asistente-no-disponible");

    @ExceptionHandler(EntradaInvalidaException.class)
    ProblemDetail entradaInvalida(EntradaInvalidaException ex) {
        var problema = problema(HttpStatus.BAD_REQUEST, TIPO_VALIDACION, "Datos inválidos",
                "La solicitud tiene errores de validación. Revisa los campos indicados.");
        problema.setProperty("errores", ex.errores());
        return problema;
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ProblemDetail recursoNoEncontrado(RecursoNoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, TIPO_NO_ENCONTRADO, "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    ProblemDetail conflicto(ConflictoException ex) {
        return problema(HttpStatus.CONFLICT, TIPO_CONFLICTO, "Conflicto", ex.getMessage());
    }

    @ExceptionHandler(ExistenciasInsuficientesException.class)
    ProblemDetail existenciasInsuficientes(ExistenciasInsuficientesException ex) {
        var problema = problema(HttpStatus.CONFLICT, TIPO_EXISTENCIAS, "Existencias insuficientes", ex.getMessage());
        problema.setProperty("faltantes", ex.faltantes());
        return problema;
    }

    @ExceptionHandler(FirmaInvalidaException.class)
    ProblemDetail firmaInvalida(FirmaInvalidaException ex) {
        log.warn("Evento de pasarela rechazado: {}", ex.getMessage());
        return problema(HttpStatus.UNAUTHORIZED, TIPO_NO_AUTENTICADO, "Firma inválida", "La firma del evento no es válida.");
    }

    @ExceptionHandler(NoAutenticadoException.class)
    ProblemDetail noAutenticado(NoAutenticadoException ex) {
        return problema(HttpStatus.UNAUTHORIZED, TIPO_NO_AUTENTICADO, "No autenticado", ex.getMessage());
    }

    /** Bloqueo optimista (@Version): alguien guardó cambios en el mismo registro entre medias. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail edicionConcurrente(ObjectOptimisticLockingFailureException ex) {
        return problema(HttpStatus.CONFLICT, TIPO_CONFLICTO, "Conflicto",
                "Otra persona modificó estos datos mientras tanto. Recarga la página e inténtalo de nuevo.");
    }

    /** Última barrera de las restricciones de la base de datos (únicos, CHECK). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail violacionDeIntegridad(DataIntegrityViolationException ex) {
        log.warn("Restricción de base de datos violada: {}", ex.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, TIPO_CONFLICTO, "Conflicto",
                "Los datos entran en conflicto con otros ya registrados.");
    }

    @ExceptionHandler(DemasiadosIntentosException.class)
    ProblemDetail demasiadosIntentos(DemasiadosIntentosException ex) {
        return problema(HttpStatus.TOO_MANY_REQUESTS, TIPO_DEMASIADOS_INTENTOS, "Demasiados intentos", ex.getMessage());
    }

    @ExceptionHandler(AsistenteNoDisponibleException.class)
    ProblemDetail asistenteNoDisponible(AsistenteNoDisponibleException ex) {
        return problema(HttpStatus.SERVICE_UNAVAILABLE, TIPO_ASISTENTE_NO_DISPONIBLE, "Asistente no disponible",
                ex.getMessage());
    }

    @ExceptionHandler(OrigenNoPermitidoException.class)
    ProblemDetail origenNoPermitido(OrigenNoPermitidoException ex) {
        return problema(HttpStatus.FORBIDDEN, TIPO_ORIGEN, "Origen no permitido",
                "La solicitud no proviene de la tienda de Nalu.");
    }

    /**
     * Spring Security traduce estas excepciones a 401/403 en su propio filtro. Si el manejador
     * genérico las capturara, un acceso denegado acabaría como 500.
     */
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    void relanzarErrorDeSeguridad(RuntimeException ex) {
        throw ex;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail errorInesperado(Exception ex) {
        log.error("Error no controlado al atender la solicitud", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, TIPO_INTERNO, "Error interno",
                "Ocurrió un error inesperado. Intenta de nuevo más tarde.");
    }

    private static ProblemDetail problema(HttpStatus estado, URI tipo, String titulo, String detalle) {
        var problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setType(tipo);
        problema.setTitle(titulo);
        return problema;
    }
}
