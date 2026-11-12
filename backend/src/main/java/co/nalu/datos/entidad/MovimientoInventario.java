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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Movimiento manual de inventario sobre una variante (RF13).
 *
 * <p>Es inmutable: se registra una vez y no se modifica. {@code cantidad} es el cambio con signo
 * aplicado a la existencia ({@code ENTRADA > 0}, {@code SALIDA < 0}) y {@code AJUSTE} admite ambos
 * signos.
 */
@Entity
@Table(name = "movimiento_inventario",
        indexes = @Index(name = "idx_movimiento_inventario_variante", columnList = "id_variante"))
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_variante", nullable = false,
            foreignKey = @ForeignKey(name = "fk_movimiento_inventario_variante"))
    private VarianteProducto variante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario",
            foreignKey = @ForeignKey(name = "fk_movimiento_inventario_administrador"))
    private Administrador administrador;

    /** Pedido que originó el movimiento (salida por venta, reintegro por cancelación). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", foreignKey = @ForeignKey(name = "fk_movimiento_inventario_pedido"))
    private Pedido pedido;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private TipoMovimiento tipo;

    @Column(nullable = false)
    private int cantidad;

    @NotBlank
    @Size(max = 160)
    @Column(length = 160, nullable = false)
    private String motivo;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime fecha;

    /** Constructor protegido requerido por JPA. */
    protected MovimientoInventario() {
    }

    /**
     * Registra un movimiento de inventario.
     *
     * @param variante       variante afectada
     * @param administrador  administrador responsable (opcional si lo genera el sistema)
     * @param tipo           tipo de movimiento
     * @param cantidad       cambio con signo aplicado a la existencia
     * @param motivo         motivo legible, máximo 160 caracteres
     */
    public MovimientoInventario(VarianteProducto variante, Administrador administrador,
                                TipoMovimiento tipo, int cantidad, String motivo) {
        this.variante = Objects.requireNonNull(variante, "variante");
        this.administrador = administrador;
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.cantidad = cantidad;
        this.motivo = Objects.requireNonNull(motivo, "motivo");
    }

    /** Movimiento generado por el sistema a partir de un pedido (sin administrador). */
    public static MovimientoInventario porPedido(VarianteProducto variante, Pedido pedido, TipoMovimiento tipo,
                                                 int cantidad, String motivo) {
        var m = new MovimientoInventario(variante, null, tipo, cantidad, motivo);
        m.pedido = Objects.requireNonNull(pedido, "pedido");
        return m;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public Long getId() {
        return id;
    }

    public VarianteProducto getVariante() {
        return variante;
    }

    public Administrador getAdministrador() {
        return administrador;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public int getCantidad() {
        return cantidad;
    }

    public String getMotivo() {
        return motivo;
    }

    public OffsetDateTime getFecha() {
        return fecha;
    }
}
