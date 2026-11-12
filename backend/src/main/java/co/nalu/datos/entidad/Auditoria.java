package co.nalu.datos.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Registro de auditoría (RNF15): quién, cuándo y qué hizo en el panel, y cada cambio de precio.
 * Inmutable y sin borrado (retención mínima de un año).
 */
@Entity
@Table(name = "auditoria", indexes = {
        @Index(name = "idx_auditoria_fecha", columnList = "fecha"),
        @Index(name = "idx_auditoria_recurso", columnList = "recurso, id_recurso")
})
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", foreignKey = @ForeignKey(name = "fk_auditoria_usuario"))
    private Usuario usuario;

    /** Qué se hizo, p. ej. {@code POST /api/v1/admin/productos} o {@code CAMBIO_PRECIO}. */
    @Column(nullable = false, length = 120)
    private String accion;

    @Column(length = 40)
    private String recurso;

    @Column(name = "id_recurso", length = 40)
    private String idRecurso;

    @Column(length = 500)
    private String detalle;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime fecha;

    protected Auditoria() {
    }

    public Auditoria(Usuario usuario, String accion, String recurso, String idRecurso, String detalle) {
        this.usuario = usuario;
        this.accion = Objects.requireNonNull(accion, "accion");
        this.recurso = recurso;
        this.idRecurso = idRecurso;
        this.detalle = detalle == null || detalle.length() <= 500 ? detalle : detalle.substring(0, 500);
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getAccion() { return accion; }
    public String getRecurso() { return recurso; }
    public String getIdRecurso() { return idRecurso; }
    public String getDetalle() { return detalle; }
    public OffsetDateTime getFecha() { return fecha; }
}
