package co.nalu.negocio.cuentas;

import co.nalu.negocio.cuentas.dto.SesionDto;
import java.time.Duration;

/**
 * Resultado de un login o refresh: la sesión para el cuerpo de la respuesta y el refresh token en
 * claro para la cookie. El valor en claro solo existe aquí; en la base de datos se guarda su hash.
 *
 * @param vidaRefresh tiempo hasta que expira el refresh token ({@code Max-Age} de la cookie)
 */
public record SesionEmitida(SesionDto sesion, String refreshToken, Duration vidaRefresh) {

    @Override
    public String toString() {
        return "SesionEmitida[sesion=" + sesion + ", vidaRefresh=" + vidaRefresh + "]";
    }
}
