package co.nalu.datos.entidad;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Pedido como agregado (plan §5.3): sus detalles, pagos e historial se guardan con él. Los precios
 * de los detalles son históricos: un cambio posterior del catálogo no altera el pedido.
 */
@Entity
@Table(name = "pedido",
        uniqueConstraints = @UniqueConstraint(name = "uk_pedido_numero", columnNames = "numero_pedido"),
        indexes = {
                @Index(name = "idx_pedido_cliente", columnList = "id_usuario"),
                @Index(name = "idx_pedido_estado_fecha", columnList = "estado, fecha"),
                @Index(name = "idx_pedido_reserva", columnList = "reserva_activa, reserva_expira_en")
        })
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Long id;

    @Column(nullable = false, length = 20)
    private String numeroPedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, foreignKey = @ForeignKey(name = "fk_pedido_cliente"))
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_direccion", nullable = false, foreignKey = @ForeignKey(name = "fk_pedido_direccion"))
    private Direccion direccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_promocion", foreignKey = @ForeignKey(name = "fk_pedido_promocion"))
    private Promocion promocion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private EstadoPedido estado = EstadoPedido.PENDIENTE_PAGO;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<DetallePedido> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<Pago> pagos = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<HistorialEstadoPedido> historial = new ArrayList<>();

    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL)
    private Envio envio;

    @NotNull @DecimalMin("0.00") @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @NotNull @DecimalMin("0.00") @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    @NotNull @DecimalMin("0.00") @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal costoEnvio = BigDecimal.ZERO;

    @NotNull @DecimalMin("0.00") @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(nullable = false)
    private boolean reservaActiva = true;

    private OffsetDateTime reservaExpiraEn;

    @Column(length = 255)
    private String motivoCancelacion;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime fecha;

    @Version
    private long version;

    protected Pedido() {
    }

    public Pedido(String numeroPedido, Cliente cliente, Direccion direccion, Promocion promocion,
                  OffsetDateTime reservaExpiraEn) {
        this.numeroPedido = Objects.requireNonNull(numeroPedido, "numeroPedido");
        this.cliente = Objects.requireNonNull(cliente, "cliente");
        this.direccion = Objects.requireNonNull(direccion, "direccion");
        this.promocion = promocion;
        this.reservaExpiraEn = reservaExpiraEn;
    }

    public void agregarDetalle(DetallePedido detalle) {
        detalle.asignarPedido(this);
        detalles.add(detalle);
    }

    public void fijarTotales(BigDecimal subtotal, BigDecimal descuento, BigDecimal costoEnvio) {
        this.subtotal = subtotal;
        this.descuento = descuento;
        this.costoEnvio = costoEnvio;
        this.total = subtotal.subtract(descuento).add(costoEnvio);
    }

    public Pago iniciarPago(String referencia) {
        var pago = new Pago(this, referencia, total);
        pagos.add(pago);
        return pago;
    }

    /**
     * Cambia el estado si la transición está permitida (plan §6.6) y deja constancia en el
     * historial. {@code responsable} es {@code null} cuando lo hace el sistema.
     */
    public void cambiarEstado(EstadoPedido nuevo, Usuario responsable, String nota) {
        if (!estado.puedeTransicionarA(nuevo)) {
            throw new IllegalStateException("No se puede pasar de " + estado + " a " + nuevo);
        }
        historial.add(new HistorialEstadoPedido(this, estado, nuevo, responsable, nota));
        this.estado = nuevo;
        if (nuevo == EstadoPedido.CANCELADO) {
            this.motivoCancelacion = nota;
        }
    }

    /** Registra el estado inicial en el historial (lo llama el checkout al crear el pedido). */
    public void registrarCreacion() {
        historial.add(new HistorialEstadoPedido(this, null, estado, cliente, "Pedido creado"));
    }

    public void registrarEnvio(Envio envio) {
        this.envio = Objects.requireNonNull(envio, "envio");
    }

    public void desactivarReserva() {
        this.reservaActiva = false;
    }

    public void renovarReserva(OffsetDateTime expiraEn) {
        this.reservaActiva = true;
        this.reservaExpiraEn = expiraEn;
    }

    @AssertTrue(message = "El total no coincide con subtotal − descuento + envío")
    private boolean isTotalConsistente() {
        return total == null || subtotal == null
                || total.compareTo(subtotal.subtract(descuento).add(costoEnvio)) == 0;
    }

    public Long getId() { return id; }
    public String getNumeroPedido() { return numeroPedido; }
    public Cliente getCliente() { return cliente; }
    public Direccion getDireccion() { return direccion; }
    public Promocion getPromocion() { return promocion; }
    public EstadoPedido getEstado() { return estado; }
    public List<DetallePedido> getDetalles() { return detalles; }
    public Envio getEnvio() { return envio; }
    public List<Pago> getPagos() { return pagos; }
    public List<HistorialEstadoPedido> getHistorial() { return historial; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDescuento() { return descuento; }
    public BigDecimal getCostoEnvio() { return costoEnvio; }
    public BigDecimal getTotal() { return total; }
    public boolean isReservaActiva() { return reservaActiva; }
    public OffsetDateTime getReservaExpiraEn() { return reservaExpiraEn; }
    public String getMotivoCancelacion() { return motivoCancelacion; }
    public OffsetDateTime getFecha() { return fecha; }
}
