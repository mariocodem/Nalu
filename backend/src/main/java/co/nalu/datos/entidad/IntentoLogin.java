package co.nalu.datos.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Intento de inicio de sesión, exitoso o fallido. Sirve para bloquear temporalmente un correo tras
 * varios fallos (plan §8.5) y como registro de auditoría (RNF15). Es inmutable.
 */
@Entity
@Table(name = "intento_login", indexes = @Index(name = "idx_intento_login_correo_fecha", columnList = "correo, fecha"))
public class IntentoLogin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_intento_login")
    private Long id;

    /** Correo normalizado tal como se intentó, exista o no una cuenta con él. */
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String correo;

    @Column(nullable = false)
    private boolean exitoso;

    @Size(max = 45)
    @Column(length = 45)
    private String ip;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime fecha;

    protected IntentoLogin() {
    }

    public IntentoLogin(String correo, boolean exitoso, String ip, OffsetDateTime fecha) {
        this.correo = Objects.requireNonNull(correo, "correo");
        this.exitoso = exitoso;
        this.ip = ip;
        this.fecha = Objects.requireNonNull(fecha, "fecha");
    }

    public Long getId() {
        return id;
    }

    public String getCorreo() {
        return correo;
    }

    public boolean isExitoso() {
        return exitoso;
    }

    public String getIp() {
        return ip;
    }

    public OffsetDateTime getFecha() {
        return fecha;
    }
}
