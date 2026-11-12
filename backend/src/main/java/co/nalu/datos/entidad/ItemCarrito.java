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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Línea del carrito. {@code precioUnitario} guarda el último precio mostrado; el subtotal siempre
 * se recalcula con el precio vigente (RF08).
 */
@Entity
@Table(name = "item_carrito",
        uniqueConstraints = @UniqueConstraint(name = "uk_item_carrito_variante", columnNames = {"id_carrito", "id_variante"}))
public class ItemCarrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_carrito", nullable = false, foreignKey = @ForeignKey(name = "fk_item_carrito_carrito"))
    private Carrito carrito;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_variante", nullable = false, foreignKey = @ForeignKey(name = "fk_item_carrito_variante"))
    private VarianteProducto variante;

    @Min(1)
    @Max(Carrito.MAXIMO_POR_ITEM)
    @Column(nullable = false)
    private int cantidad;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    protected ItemCarrito() {
    }

    ItemCarrito(Carrito carrito, VarianteProducto variante, int cantidad, BigDecimal precioUnitario) {
        this.carrito = Objects.requireNonNull(carrito, "carrito");
        this.variante = Objects.requireNonNull(variante, "variante");
        this.cantidad = cantidad;
        this.precioUnitario = Objects.requireNonNull(precioUnitario, "precioUnitario");
    }

    public void cambiarCantidad(int cantidad, BigDecimal precioVigente) {
        if (cantidad < 1 || cantidad > Carrito.MAXIMO_POR_ITEM) {
            throw new IllegalArgumentException("La cantidad debe estar entre 1 y " + Carrito.MAXIMO_POR_ITEM);
        }
        this.cantidad = cantidad;
        this.precioUnitario = Objects.requireNonNull(precioVigente, "precioVigente");
    }

    public void actualizarPrecio(BigDecimal precioVigente) {
        this.precioUnitario = Objects.requireNonNull(precioVigente, "precioVigente");
    }

    public Long getId() {
        return id;
    }

    public Carrito getCarrito() {
        return carrito;
    }

    public VarianteProducto getVariante() {
        return variante;
    }

    public int getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }
}
