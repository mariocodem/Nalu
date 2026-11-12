package co.nalu.negocio.comun;

/**
 * No se pudo autenticar a quien hace la solicitud: credenciales incorrectas, cuenta bloqueada o
 * sesión (refresh token) inválida. La API responde 401.
 *
 * <p>El mensaje se muestra al usuario final: no debe revelar si un correo existe.
 */
public class NoAutenticadoException extends RuntimeException {

    public NoAutenticadoException(String mensaje) {
        super(mensaje);
    }
}
