package co.nalu.datos.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Cuenta de un administrador de Nalu.
 *
 * <p>Comparte la clave primaria con {@link Usuario} y registra la fecha de su último acceso.
 */
@Entity
@Table(name = "administrador")
@DiscriminatorValue("ADMINISTRADOR")
@PrimaryKeyJoinColumn(name = "id_usuario", foreignKey = @ForeignKey(name = "fk_administrador_usuario"))
public class Administrador extends Usuario {

    @Column(name = "ultimo_acceso")
    private OffsetDateTime ultimoAcceso;

    /** Constructor protegido requerido por JPA. */
    protected Administrador() {
    }

    /**
     * Crea un administrador con los datos obligatorios.
     *
     * @param nombre         nombre de pila
     * @param apellido       apellido
     * @param correo         correo electrónico único
     * @param contrasenaHash hash de la contraseña
     */
    public Administrador(String nombre, String apellido, String correo, String contrasenaHash) {
        super(nombre, apellido, correo, contrasenaHash);
    }

    /** Actualiza la marca del último acceso al panel. */
    public void registrarAcceso(OffsetDateTime cuando) {
        this.ultimoAcceso = Objects.requireNonNull(cuando, "cuando");
    }

    public OffsetDateTime getUltimoAcceso() {
        return ultimoAcceso;
    }
}
