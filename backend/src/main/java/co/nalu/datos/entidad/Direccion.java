package co.nalu.datos.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * Dirección de envío de un cliente.
 *
 * <p>No se borra: se desactiva, porque puede estar referenciada por pedidos anteriores
 * (contrato de la API §2).
 */
@Entity
@Table(name = "direccion")
public class Direccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_direccion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false,
            foreignKey = @ForeignKey(name = "fk_direccion_cliente"))
    private Cliente cliente;

    @NotBlank
    @Size(max = 40)
    @Column(length = 40, nullable = false)
    private String etiqueta;

    @NotBlank
    @Size(max = 120)
    @Column(length = 120, nullable = false)
    private String destinatario;

    @NotBlank
    @Size(max = 60)
    @Column(length = 60, nullable = false)
    private String departamento;

    @NotBlank
    @Size(max = 60)
    @Column(length = 60, nullable = false)
    private String ciudad;

    @NotBlank
    @Size(max = 160)
    @Column(length = 160, nullable = false)
    private String direccion;

    @Size(max = 12)
    @Column(length = 12)
    private String codigoPostal;

    @NotBlank
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String telefono;

    @Column(nullable = false)
    private boolean activa = true;

    /** Constructor protegido requerido por JPA. */
    protected Direccion() {
    }

    /**
     * Crea una dirección de envío.
     *
     * @param cliente       cliente dueño de la dirección
     * @param etiqueta      nombre corto («Casa», «Oficina»), máximo 40 caracteres
     * @param destinatario  persona que recibe, máximo 120 caracteres
     * @param departamento  departamento, máximo 60 caracteres
     * @param ciudad        ciudad, máximo 60 caracteres
     * @param direccion     dirección completa, máximo 160 caracteres
     * @param codigoPostal  código postal (opcional), máximo 12 caracteres
     * @param telefono      teléfono de contacto, máximo 20 caracteres
     */
    public Direccion(Cliente cliente, String etiqueta, String destinatario, String departamento,
                     String ciudad, String direccion, String codigoPostal, String telefono) {
        this.cliente = Objects.requireNonNull(cliente, "cliente");
        this.etiqueta = Objects.requireNonNull(etiqueta, "etiqueta");
        this.destinatario = Objects.requireNonNull(destinatario, "destinatario");
        this.departamento = Objects.requireNonNull(departamento, "departamento");
        this.ciudad = Objects.requireNonNull(ciudad, "ciudad");
        this.direccion = Objects.requireNonNull(direccion, "direccion");
        this.codigoPostal = codigoPostal;
        this.telefono = Objects.requireNonNull(telefono, "telefono");
    }

    /** Cambia los datos editables desde la cuenta del cliente. */
    public void actualizar(String etiqueta, String destinatario, String departamento, String ciudad,
                           String direccion, String codigoPostal, String telefono) {
        this.etiqueta = Objects.requireNonNull(etiqueta, "etiqueta");
        this.destinatario = Objects.requireNonNull(destinatario, "destinatario");
        this.departamento = Objects.requireNonNull(departamento, "departamento");
        this.ciudad = Objects.requireNonNull(ciudad, "ciudad");
        this.direccion = Objects.requireNonNull(direccion, "direccion");
        this.codigoPostal = codigoPostal;
        this.telefono = Objects.requireNonNull(telefono, "telefono");
    }

    public void desactivar() {
        this.activa = false;
    }

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public String getDepartamento() {
        return departamento;
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public String getTelefono() {
        return telefono;
    }

    public boolean isActiva() {
        return activa;
    }
}
