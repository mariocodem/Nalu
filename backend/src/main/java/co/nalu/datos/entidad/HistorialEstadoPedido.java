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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;

/** Trazabilidad de cada cambio de estado del pedido (RF14, RNF15). Inmutable. */
@Entity
@Table(name = "historial_estado_pedido")
public class HistorialEstadoPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false, foreignKey = @ForeignKey(name = "fk_historial_estado_pedido_pedido"))
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(length = 25)
    private EstadoPedido estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private EstadoPedido estadoNuevo;

    /** {@code null} cuando el cambio lo hizo el sistema (webhook, expiración). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", foreignKey = @ForeignKey(name = "fk_historial_estado_pedido_usuario"))
    private Usuario usuario;

    @Column(length = 255)
    private String nota;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime fecha;

    protected HistorialEstadoPedido() {
    }

    HistorialEstadoPedido(Pedido pedido, EstadoPedido anterior, EstadoPedido nuevo, Usuario usuario, String nota) {
        this.pedido = Objects.requireNonNull(pedido, "pedido");
        this.estadoAnterior = anterior;
        this.estadoNuevo = Objects.requireNonNull(nuevo, "nuevo");
        this.usuario = usuario;
        this.nota = nota;
    }

    public Long getId() { return id; }
    public EstadoPedido getEstadoAnterior() { return estadoAnterior; }
    public EstadoPedido getEstadoNuevo() { return estadoNuevo; }
    public Usuario getUsuario() { return usuario; }
    public String getNota() { return nota; }
    public OffsetDateTime getFecha() { return fecha; }
}
