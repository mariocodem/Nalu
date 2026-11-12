package co.nalu.negocio.cuentas;

import co.nalu.datos.entidad.IntentoLogin;
import co.nalu.datos.repositorio.IntentoLoginRepository;
import co.nalu.negocio.comun.DemasiadosIntentosException;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

/**
 * Bloqueo temporal del login por correo (plan §8.5): con {@value #MAXIMO_FALLOS} fallos en los últimos
 * 15 minutos (ventana deslizante, contados desde el último login correcto) se rechaza el login hasta
 * que el más antiguo de esos fallos salga de la ventana. Mientras el correo está bloqueado no se registran intentos, para que
 * insistir no alargue el bloqueo.
 *
 * <p>Se usa dentro de la transacción de {@link AutenticacionService#iniciarSesion}.
 */
@Component
class ProteccionFuerzaBruta {

    static final int MAXIMO_FALLOS = 5;
    static final Duration VENTANA = Duration.ofMinutes(15);
    static final String MENSAJE = "Demasiados intentos fallidos. Espera unos minutos e inténtalo de nuevo.";

    private final IntentoLoginRepository intentos;

    ProteccionFuerzaBruta(IntentoLoginRepository intentos) {
        this.intentos = intentos;
    }

    void verificarQueNoEsteBloqueado(String correo, OffsetDateTime ahora) {
        var desde = ahora.minus(VENTANA);
        var ultimoExito = intentos.findFirstByCorreoAndExitosoTrueOrderByFechaDesc(correo)
                .map(IntentoLogin::getFecha)
                .filter(fecha -> fecha.isAfter(desde))
                .orElse(desde);
        if (intentos.countByCorreoAndExitosoFalseAndFechaAfter(correo, ultimoExito) >= MAXIMO_FALLOS) {
            throw new DemasiadosIntentosException(MENSAJE);
        }
    }

    void registrar(String correo, boolean exitoso, String ip, OffsetDateTime ahora) {
        intentos.save(new IntentoLogin(correo, exitoso, ip, ahora));
    }
}
