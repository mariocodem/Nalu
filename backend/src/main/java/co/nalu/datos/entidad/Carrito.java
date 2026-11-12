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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Carrito de compras (RF08). El de un visitante se identifica por {@code token} (header
 * {@code X-Cart-Token}); al iniciar sesión se fusiona con el del cliente.
 */
@Entity
@Table(name = "carrito",
        uniqueConstraints = @UniqueConstraint(name = "uk_carrito_token", columnNames = "token"),
        indexes = @Index(name = "idx_carrito_cliente_estado", columnList = "id_usuario, estado"))
public class Carrito {

    public static final int MAXIMO_POR_ITEM = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carrito")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", foreignKey = @ForeignKey(name = "fk_carrito_cliente"))
    private Cliente cliente;

    @Column(nullable = false, length = 64)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCarrito estado = EstadoCarrito.ACTIVO;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime fechaActualizacion;

    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ItemCarrito> items = new ArrayList<>();

    protected Carrito() {
    }

    public Carrito(String token, Cliente cliente) {
        this.token = Objects.requireNonNull(token, "token");
        this.cliente = cliente;
    }

    /** Suma la cantidad si la variante ya está en el carrito, con el tope por ítem. */
    public ItemCarrito agregar(VarianteProducto variante, int cantidad, BigDecimal precio) {
        var existente = item(variante);
        if (existente.isPresent()) {
            existente.get().cambiarCantidad(Math.min(existente.get().getCantidad() + cantidad, MAXIMO_POR_ITEM), precio);
            return existente.get();
        }
        var item = new ItemCarrito(this, variante, Math.min(cantidad, MAXIMO_POR_ITEM), precio);
        items.add(item);
        return item;
    }

    public void quitar(ItemCarrito item) {
        items.remove(item);
    }

    public Optional<ItemCarrito> item(VarianteProducto variante) {
        return items.stream().filter(i -> i.getVariante().getId().equals(variante.getId())).findFirst();
    }

    public void asignarCliente(Cliente cliente) {
        this.cliente = Objects.requireNonNull(cliente, "cliente");
    }

    /** Pasa los ítems de otro carrito a este y marca el otro como fusionado. */
    public void absorber(Carrito otro) {
        for (var item : List.copyOf(otro.items)) {
            agregar(item.getVariante(), item.getCantidad(), item.getPrecioUnitario());
        }
        otro.items.clear();
        otro.estado = EstadoCarrito.FUSIONADO;
    }

    public void convertir() {
        this.estado = EstadoCarrito.CONVERTIDO;
    }

    public void vaciar() {
        items.clear();
    }

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public String getToken() {
        return token;
    }

    public EstadoCarrito getEstado() {
        return estado;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public List<ItemCarrito> getItems() {
        return items;
    }
}
