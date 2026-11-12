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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Intento de pago de un pedido con la pasarela (RF12). Un pedido puede tener varios intentos y
 * solo uno aprobado. {@code referencia} es nuestra y única por intento; {@code referenciaExterna}
 * es el id de la transacción en Wompi.
 */
@Entity
@Table(name = "pago", uniqueConstraints = @UniqueConstraint(name = "uk_pago_referencia", columnNames = "referencia"))
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false, foreignKey = @ForeignKey(name = "fk_pago_pedido"))
    private Pedido pedido;

    @Column(length = 30)
    private String metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private EstadoPago estado = EstadoPago.INICIADO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 60)
    private String referencia;

    @Column(length = 80)
    private String referenciaExterna;

    private OffsetDateTime fechaTransaccion;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    protected Pago() {
    }

    Pago(Pedido pedido, String referencia, BigDecimal monto) {
        this.pedido = Objects.requireNonNull(pedido, "pedido");
        this.referencia = Objects.requireNonNull(referencia, "referencia");
        this.monto = Objects.requireNonNull(monto, "monto");
    }

    public void registrarResultado(EstadoPago estado, String referenciaExterna, String metodo, OffsetDateTime cuando) {
        this.estado = Objects.requireNonNull(estado, "estado");
        this.referenciaExterna = referenciaExterna;
        this.metodo = metodo;
        this.fechaTransaccion = cuando;
    }

    public Long getId() { return id; }
    public Pedido getPedido() { return pedido; }
    public String getMetodo() { return metodo; }
    public EstadoPago getEstado() { return estado; }
    public BigDecimal getMonto() { return monto; }
    public String getReferencia() { return referencia; }
    public String getReferenciaExterna() { return referenciaExterna; }
    public OffsetDateTime getFechaTransaccion() { return fechaTransaccion; }
    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }
}
