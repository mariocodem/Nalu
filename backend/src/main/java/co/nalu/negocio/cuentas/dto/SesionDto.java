package co.nalu.negocio.cuentas.dto;

import java.time.Instant;

/** Respuesta de login y refresh (docs/arquitectura/api-autenticacion.md). */
public record SesionDto(String accessToken, Instant expiraEn, UsuarioSesionDto usuario) {

    @Override
    public String toString() {
        return "SesionDto[expiraEn=" + expiraEn + ", usuario=" + usuario + "]";
    }
}
