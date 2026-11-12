package co.nalu.datos.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Cuenta de un cliente de la tienda.
 *
 * <p>Comparte la clave primaria con {@link Usuario} mediante herencia JOINED y expone sus
 * direcciones de envío en modo de solo lectura: el agregado que las modifica es {@link Direccion}.
 */
@Entity
@Table(name = "cliente")
@DiscriminatorValue("CLIENTE")
@PrimaryKeyJoinColumn(name = "id_usuario", foreignKey = @ForeignKey(name = "fk_cliente_usuario"))
public class Cliente extends Usuario {

    @Size(max = 20)
    private String documento;

    private boolean aceptaComunicaciones;

    @NotNull
    @Column(nullable = false)
    private OffsetDateTime politicaAceptadaEn;

    @OneToMany(mappedBy = "cliente", fetch = FetchType.LAZY)
    private List<Direccion> direcciones = new ArrayList<>();

    /** Constructor protegido requerido por JPA. */
    protected Cliente() {
    }

    /**
     * Crea un cliente con los datos obligatorios y sin documento ni consentimiento comercial.
     *
     * @param nombre              nombre de pila
     * @param apellido            apellido
     * @param correo              correo electrónico único
     * @param contrasenaHash      hash de la contraseña
     * @param politicaAceptadaEn  fecha de aceptación de la política de datos (Ley 1581, RNF05)
     */
    public Cliente(String nombre, String apellido, String correo, String contrasenaHash,
                   OffsetDateTime politicaAceptadaEn) {
        this(nombre, apellido, correo, contrasenaHash, null, false, politicaAceptadaEn);
    }

    /**
     * Crea un cliente con todos sus datos iniciales.
     *
     * @param nombre                 nombre de pila
     * @param apellido               apellido
     * @param correo                 correo electrónico único
     * @param contrasenaHash         hash de la contraseña
     * @param documento              documento de identidad (opcional)
     * @param aceptaComunicaciones   consentimiento para comunicaciones comerciales
     * @param politicaAceptadaEn     fecha de aceptación de la política de datos (Ley 1581, RNF05)
     */
    public Cliente(String nombre, String apellido, String correo, String contrasenaHash,
                   String documento, boolean aceptaComunicaciones,
                   OffsetDateTime politicaAceptadaEn) {
        super(nombre, apellido, correo, contrasenaHash);
        this.documento = documento;
        this.aceptaComunicaciones = aceptaComunicaciones;
        this.politicaAceptadaEn = Objects.requireNonNull(politicaAceptadaEn, "politicaAceptadaEn");
    }

    /**
     * Crea un cliente con teléfono, que el registro exige (contrato de la API §1).
     *
     * @param nombre                 nombre de pila
     * @param apellido               apellido
     * @param correo                 correo electrónico único
     * @param telefono               teléfono de contacto (obligatorio en el registro)
     * @param contrasenaHash         hash de la contraseña
     * @param documento              documento de identidad (opcional)
     * @param aceptaComunicaciones   consentimiento para comunicaciones comerciales
     * @param politicaAceptadaEn     fecha de aceptación de la política de datos (Ley 1581, RNF05)
     */
    public Cliente(String nombre, String apellido, String correo, String telefono,
                   String contrasenaHash, String documento, boolean aceptaComunicaciones,
                   OffsetDateTime politicaAceptadaEn) {
        super(nombre, apellido, correo, contrasenaHash);
        actualizarDatosPersonales(nombre, apellido, telefono);
        this.documento = documento;
        this.aceptaComunicaciones = aceptaComunicaciones;
        this.politicaAceptadaEn = Objects.requireNonNull(politicaAceptadaEn, "politicaAceptadaEn");
    }

    /** Cambia los datos personales editables desde el perfil (el correo no se cambia en F1). */
    public void actualizarPerfil(String nombre, String apellido, String telefono,
                                 String documento, boolean aceptaComunicaciones) {
        actualizarDatosPersonales(nombre, apellido, telefono);
        this.documento = documento;
        this.aceptaComunicaciones = aceptaComunicaciones;
    }

    public String getDocumento() {
        return documento;
    }

    public boolean isAceptaComunicaciones() {
        return aceptaComunicaciones;
    }

    public OffsetDateTime getPoliticaAceptadaEn() {
        return politicaAceptadaEn;
    }

    /** Copia de solo lectura: las direcciones se gestionan a través de su propia entidad. */
    public List<Direccion> getDirecciones() {
        return List.copyOf(direcciones);
    }
}
