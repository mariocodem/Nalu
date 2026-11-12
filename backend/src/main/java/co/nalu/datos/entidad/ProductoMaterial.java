package co.nalu.datos.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Asociación entre un producto y un material, con el papel que cumple dentro de la joya.
 *
 * <p>Usa clave primaria compuesta ({@code id_producto}, {@code id_material}) con
 * {@link EmbeddedId} y {@link MapsId} (plan de desarrollo §5.3).
 */
@Entity
@Table(name = "producto_material")
public class ProductoMaterial {

    @EmbeddedId
    private ProductoMaterialId id;

    @MapsId("idProducto")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false,
            foreignKey = @ForeignKey(name = "fk_producto_material_producto"))
    private Producto producto;

    @MapsId("idMaterial")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_material", nullable = false,
            foreignKey = @ForeignKey(name = "fk_producto_material_material"))
    private Material material;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private RolMaterial rol;

    @Column(precision = 8, scale = 2)
    private BigDecimal pesoGramos;

    @Column(precision = 6, scale = 2)
    private BigDecimal quilates;

    /** Constructor protegido requerido por JPA. */
    protected ProductoMaterial() {
    }

    /**
     * Crea la asociación entre un producto y un material.
     *
     * @param producto    producto al que pertenece el material
     * @param material    material asociado
     * @param rol         papel del material dentro del producto
     * @param pesoGramos  peso en gramos (opcional)
     * @param quilates    quilates (opcional)
     */
    public ProductoMaterial(Producto producto, Material material, RolMaterial rol,
                            BigDecimal pesoGramos, BigDecimal quilates) {
        Objects.requireNonNull(producto, "producto");
        Objects.requireNonNull(material, "material");
        this.id = new ProductoMaterialId(producto.getId(), material.getId());
        this.producto = producto;
        this.material = material;
        this.rol = Objects.requireNonNull(rol, "rol");
        this.pesoGramos = pesoGramos;
        this.quilates = quilates;
    }

    /** Enlaza la asociación con su producto (lo usa el agregado al reemplazar materiales). */
    void actualizar(RolMaterial rol, BigDecimal pesoGramos, BigDecimal quilates) {
        this.rol = Objects.requireNonNull(rol, "rol");
        this.pesoGramos = pesoGramos;
        this.quilates = quilates;
    }

    void asignarProducto(Producto producto) {
        this.producto = Objects.requireNonNull(producto, "producto");
    }

    public ProductoMaterialId getId() {
        return id;
    }

    public Producto getProducto() {
        return producto;
    }

    public Material getMaterial() {
        return material;
    }

    public RolMaterial getRol() {
        return rol;
    }

    public BigDecimal getPesoGramos() {
        return pesoGramos;
    }

    public BigDecimal getQuilates() {
        return quilates;
    }
}
