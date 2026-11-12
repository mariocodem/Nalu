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
import jakarta.persistence.Version;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Objects;
import org.hibernate.annotations.Check;

/**
 * Variante concreta de un producto (talla, peso y existencia propia).
 *
 * <p>Protege las invariantes de inventario RNF10: ninguna existencia ni reserva negativa y las
 * reservas nunca superan la existencia. El bloqueo optimista evita ediciones concurrentes desde el
 * panel (plan de desarrollo §5.2 y §5.3).
 */
@Entity
@Table(name = "variante_producto",
        uniqueConstraints = @UniqueConstraint(name = "uk_variante_producto_sku", columnNames = "sku"))
@Check(constraints = "stock_actual >= 0 AND stock_reservado >= 0 AND stock_reservado <= stock_actual")
public class VarianteProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_variante")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false,
            foreignKey = @ForeignKey(name = "fk_variante_producto_producto"))
    private Producto producto;

    @NotBlank
    @Size(max = 40)
    @Column(length = 40, nullable = false)
    private String sku;

    @Size(max = 20)
    @Column(length = 20)
    private String talla;

    @Column(precision = 8, scale = 2)
    private BigDecimal pesoGramos;

    @NotNull
    @DecimalMin("0.00")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal precioAdicional = BigDecimal.ZERO;

    @Min(0)
    @Column(nullable = false)
    private int stockActual;

    @Min(0)
    @Column(nullable = false)
    private int stockReservado;

    @Column(nullable = false)
    private boolean activa = true;

    @Version
    private long version;

    /** Constructor protegido requerido por JPA. */
    protected VarianteProducto() {
    }

    /**
     * Crea una variante con su existencia inicial.
     *
     * @param producto        producto al que pertenece
     * @param sku             SKU único de la variante, máximo 40 caracteres
     * @param talla           talla (opcional), máximo 20 caracteres
     * @param pesoGramos      peso en gramos (opcional)
     * @param precioAdicional sobreprecio sobre el producto, mayor o igual a cero
     * @param existenciaInicial unidades disponibles al crearla
     */
    public VarianteProducto(Producto producto, String sku, String talla, BigDecimal pesoGramos,
                            BigDecimal precioAdicional, int existenciaInicial) {
        if (existenciaInicial < 0) {
            throw new IllegalArgumentException("La existencia inicial no puede ser negativa");
        }
        this.producto = Objects.requireNonNull(producto, "producto");
        this.sku = Objects.requireNonNull(sku, "sku");
        this.talla = talla;
        this.pesoGramos = pesoGramos;
        this.precioAdicional = Objects.requireNonNull(precioAdicional, "precioAdicional");
        this.stockActual = existenciaInicial;
    }

    /** Cambia los datos editables desde el panel (la existencia solo cambia con movimientos). */
    public void actualizar(String talla, BigDecimal pesoGramos, BigDecimal precioAdicional, boolean activa) {
        this.talla = talla;
        this.pesoGramos = pesoGramos;
        this.precioAdicional = Objects.requireNonNull(precioAdicional, "precioAdicional");
        this.activa = activa;
    }

    /** Enlaza la variante con su producto. */
    void asignarProducto(Producto producto) {
        this.producto = Objects.requireNonNull(producto, "producto");
    }

    // Restricción entre campos: se valida antes de cada INSERT/UPDATE.
    @AssertTrue(message = "Las unidades reservadas no pueden superar la existencia")
    private boolean isReservaConsistente() {
        return stockReservado <= stockActual;
    }

    public int disponible() {
        return stockActual - stockReservado;
    }

    /**
     * Reserva unidades para un pedido.
     *
     * @throws IllegalStateException si la cantidad no es positiva o supera lo disponible
     */
    public void reservar(int cantidad) {
        if (cantidad <= 0 || cantidad > disponible()) {
            throw new IllegalStateException("Existencias insuficientes para " + sku);
        }
        stockReservado += cantidad;
    }

    /** Libera unidades reservadas (nunca deja la reserva por debajo de cero). */
    public void liberar(int cantidad) {
        stockReservado -= Math.min(cantidad, stockReservado);
    }

    /** Confirma la salida de unidades reservadas: descuenta reserva y existencia. */
    public void confirmarSalida(int cantidad) {
        stockReservado -= cantidad;
        stockActual -= cantidad;
    }

    /**
     * Ajusta la existencia con un cambio con signo.
     *
     * @throws IllegalStateException si el ajuste dejaría reservas sin respaldo
     */
    public void ajustar(int delta) {
        if (stockActual + delta < stockReservado) {
            throw new IllegalStateException("El ajuste dejaría reservas sin respaldo");
        }
        stockActual += delta;
    }

    public Long getId() {
        return id;
    }

    public Producto getProducto() {
        return producto;
    }

    public String getSku() {
        return sku;
    }

    public String getTalla() {
        return talla;
    }

    public BigDecimal getPesoGramos() {
        return pesoGramos;
    }

    public BigDecimal getPrecioAdicional() {
        return precioAdicional;
    }

    public int getStockActual() {
        return stockActual;
    }

    public int getStockReservado() {
        return stockReservado;
    }

    public boolean isActiva() {
        return activa;
    }

    public long getVersion() {
        return version;
    }
}
