package co.nalu.api;

import org.springframework.security.oauth2.jwt.Jwt;

/** Identificador del usuario autenticado: el claim {@code sub} del access token. */
public final class UsuarioActual {

    private UsuarioActual() {}

    public static long id(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
