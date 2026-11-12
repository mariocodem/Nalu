package co.nalu.negocio.cuentas.dto;

import am.ik.yavi.core.Validator;
import co.nalu.negocio.comun.EsquemaValidacion;

/** Entrada de {@code POST /api/v1/auth/login}. */
public record LoginRequest(String correo, String contrasena) {

    /** Quita los espacios que añaden el autocompletado y los teclados móviles. */
    public LoginRequest {
        correo = correo == null ? null : correo.strip();
    }

    public static final Validator<LoginRequest> ESQUEMA = EsquemaValidacion.para(LoginRequest.class)
            ._string(LoginRequest::correo, "correo",
                    c -> c.notBlank().email().message("Ingresa un correo válido").lessThanOrEqual(120))
            ._string(LoginRequest::contrasena, "contrasena",
                    c -> c.notBlank().message("Ingresa tu contraseña").lessThanOrEqual(128))
            .build();

    /** Evita que la contraseña aparezca en logs. */
    @Override
    public String toString() {
        return "LoginRequest[correo=" + correo + "]";
    }
}
