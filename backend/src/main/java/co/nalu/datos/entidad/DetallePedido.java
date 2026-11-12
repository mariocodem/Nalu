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
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.Objects;

/** Línea del pedido con el precio del momento de la venta (Tabla 12, nota de la Figura 8). */
@Entity
@Table(name = "detalle_pedido")
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false, foreignKey = @ForeignKey(name = "fk_detalle_pedido_pedido"))
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_variante", nullable = false, foreignKey = @ForeignKey(name = "fk_detalle_pedido_variante"))
    private VarianteProducto variante;

    @Min(1)
    @Column(nullable = false)
    private int cantidad;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    /** Parte del descuento del cupón que corresponde a esta línea. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuentoAplicado = BigDecimal.ZERO;

    protected DetallePedido() {
    }

    public DetallePedido(VarianteProducto variante, int cantidad, BigDecimal precioUnitario, BigDecimal descuentoAplicado) {
        this.variante = Objects.requireNonNull(variante, "variante");
        this.cantidad = cantidad;
        this.precioUnitario = Objects.requireNonNull(precioUnitario, "precioUnitario");
        this.descuentoAplicado = Objects.requireNonNull(descuentoAplicado, "descuentoAplicado");
    }

    void asignarPedido(Pedido pedido) {
        this.pedido = Objects.requireNonNull(pedido, "pedido");
    }

    public BigDecimal getSubtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public Long getId() { return id; }
    public Pedido getPedido() { return pedido; }
    public VarianteProducto getVariante() { return variante; }
    public int getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public BigDecimal getDescuentoAplicado() { return descuentoAplicado; }
}
