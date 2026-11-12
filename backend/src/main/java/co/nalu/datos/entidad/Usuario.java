package co.nalu.datos.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Datos comunes de una cuenta de Nalu.
 *
 * <p>La jerarquía usa herencia JOINED: la tabla {@code usuario} guarda los datos comunes y las
 * tablas {@code cliente} y {@code administrador} comparten su clave primaria.
 */
@Entity
@Table(name = "usuario",
        uniqueConstraints = @UniqueConstraint(name = "uk_usuario_correo", columnNames = "correo"))
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "tipo_usuario", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @NotBlank
    @Size(max = 60)
    @Column(nullable = false)
    private String nombre;

    @NotBlank
    @Size(max = 60)
    @Column(nullable = false)
    private String apellido;

    @NotBlank
    @Email
    @Size(max = 120)
    @Column(nullable = false)
    private String correo;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String contrasenaHash;

    @Size(max = 20)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime fechaRegistro;

    /** Constructor protegido requerido por JPA. */
    protected Usuario() {
    }

    /**
     * Crea un usuario con los datos obligatorios.
     *
     * @param nombre         nombre de pila
     * @param apellido       apellido
     * @param correo         correo electrónico único
     * @param contrasenaHash hash de la contraseña (nunca la contraseña en claro)
     */
    protected Usuario(String nombre, String apellido, String correo, String contrasenaHash) {
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.apellido = Objects.requireNonNull(apellido, "apellido");
        this.correo = Objects.requireNonNull(correo, "correo");
        this.contrasenaHash = Objects.requireNonNull(contrasenaHash, "contrasenaHash");
    }

    /** Cambia los datos personales editables desde el perfil (el correo no se cambia en F1). */
    protected void actualizarDatosPersonales(String nombre, String apellido, String telefono) {
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.apellido = Objects.requireNonNull(apellido, "apellido");
        this.telefono = telefono;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getCorreo() {
        return correo;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public String getTelefono() {
        return telefono;
    }

    public EstadoUsuario getEstado() {
        return estado;
    }

    public OffsetDateTime getFechaRegistro() {
        return fechaRegistro;
    }
}
