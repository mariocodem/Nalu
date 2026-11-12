package co.nalu.negocio.cuentas.dto;

import am.ik.yavi.core.Validator;
import co.nalu.negocio.comun.EsquemaValidacion;

/** Entrada de {@code POST /api/v1/auth/registro} (RF01, plan §4.6). */
public record RegistroRequest(String nombre, String apellido, String correo, String telefono, String contrasena,
                              boolean aceptaPolitica, boolean aceptaComunicaciones) {

    static final String PATRON_TELEFONO = "^\\+?[0-9]{7,15}$";

    /** Quita espacios sobrantes; la contraseña se respeta tal cual. */
    public RegistroRequest {
        nombre = nombre == null ? null : nombre.strip();
        apellido = apellido == null ? null : apellido.strip();
        correo = correo == null ? null : correo.strip();
        telefono = telefono == null ? null : telefono.replace(" ", "");
    }

    public static final Validator<RegistroRequest> ESQUEMA = EsquemaValidacion.para(RegistroRequest.class)
            ._string(RegistroRequest::nombre, "nombre", c -> c.notBlank().lessThanOrEqual(60))
            ._string(RegistroRequest::apellido, "apellido", c -> c.notBlank().lessThanOrEqual(60))
            ._string(RegistroRequest::correo, "correo",
                    c -> c.notBlank().email().message("Ingresa un correo válido").lessThanOrEqual(120))
            ._string(RegistroRequest::telefono, "telefono", c -> c.notBlank().pattern(PATRON_TELEFONO)
                    .message("El teléfono debe tener entre 7 y 15 dígitos"))
            ._string(RegistroRequest::contrasena, "contrasena", c -> c.notBlank()
                    .greaterThanOrEqual(8).message("La contraseña debe tener al menos 8 caracteres")
                    .lessThanOrEqual(128))
            .constraintOnTarget(RegistroRequest::aceptaPolitica, "aceptaPolitica", "politica.requerida",
                    "Debes aceptar la política de tratamiento de datos")
            .build();

    @Override
    public String toString() {
        return "RegistroRequest[correo=" + correo + "]";
    }
}
