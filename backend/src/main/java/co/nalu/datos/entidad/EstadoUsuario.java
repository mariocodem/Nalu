package co.nalu.datos.entidad;

/**
 * Estado de una cuenta de usuario (plan de desarrollo §8.3, regla 3).
 *
 * <p>Se persiste como cadena para que el valor sea legible y estable en la base de datos.
 */
public enum EstadoUsuario {

    /** La cuenta puede iniciar sesión y operar con normalidad. */
    ACTIVO,

    /** La cuenta está bloqueada y no puede iniciar sesión. */
    BLOQUEADO
}
