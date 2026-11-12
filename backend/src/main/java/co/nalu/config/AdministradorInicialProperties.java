package co.nalu.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Credenciales del administrador inicial que crea la semilla en los perfiles {@code dev} y
 * {@code demo} (plan §5.6). Llegan por variables de entorno; nunca se versionan.
 */
@Validated
@ConfigurationProperties("nalu.admin")
public record AdministradorInicialProperties(@NotBlank @Email String correo,
                                             @NotBlank @Size(min = 12) String contrasena) {

    @Override
    public String toString() {
        return "AdministradorInicialProperties[correo=" + correo + "]";
    }
}
