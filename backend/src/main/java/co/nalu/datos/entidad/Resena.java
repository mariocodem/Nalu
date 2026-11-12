package co.nalu.datos.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Valoración de un cliente sobre un producto que recibió (RF07). Se publica tras moderación. */
@Entity
@Table(name = "resena",
        uniqueConstraints = @UniqueConstraint(name = "uk_resena_producto_cliente", columnNames = {"id_producto", "id_usuario"}),
        indexes = @Index(name = "idx_resena_producto_estado", columnList = "id_producto, estado"))
@Check(constraints = "calificacion BETWEEN 1 AND 5")
public class Resena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resena")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false, foreignKey = @ForeignKey(name = "fk_resena_producto"))
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, foreignKey = @ForeignKey(name = "fk_resena_cliente"))
    private Cliente cliente;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private int calificacion;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    private String comentario;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoResena estado = EstadoResena.PENDIENTE;

    protected Resena() {
    }

    public Resena(Producto producto, Cliente cliente, int calificacion, String comentario) {
        this.producto = Objects.requireNonNull(producto, "producto");
        this.cliente = Objects.requireNonNull(cliente, "cliente");
        this.calificacion = calificacion;
        this.comentario = comentario;
    }

    public void moderar(EstadoResena nuevo) {
        this.estado = Objects.requireNonNull(nuevo, "nuevo");
    }

    public Long getId() { return id; }
    public Producto getProducto() { return producto; }
    public Cliente getCliente() { return cliente; }
    public int getCalificacion() { return calificacion; }
    public String getComentario() { return comentario; }
    public OffsetDateTime getFecha() { return fecha; }
    public EstadoResena getEstado() { return estado; }
}
