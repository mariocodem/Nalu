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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Producto del catálogo con sus materiales, variantes e imágenes.
 *
 * <p>Es el agregado que protege las reglas de publicación: categoría activa, al menos un material,
 * al menos una variante activa y al menos una imagen (contrato de la API §4). Las variantes no se
 * eliminan en cascada porque una variante vendida debe conservarse por trazabilidad.
 */
@Entity
@Table(name = "producto",
        indexes = {
                @Index(name = "idx_producto_estado_precio", columnList = "estado, precio_desde"),
                @Index(name = "idx_producto_categoria", columnList = "id_categoria")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_producto_sku", columnNames = "sku"),
                @UniqueConstraint(name = "uk_producto_slug", columnNames = "slug")
        })
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false,
            foreignKey = @ForeignKey(name = "fk_producto_categoria"))
    private Categoria categoria;

    @NotBlank
    @Size(max = 40)
    @Column(length = 40, nullable = false)
    private String sku;

    @NotBlank
    @Size(max = 120)
    @Column(length = 120, nullable = false)
    private String nombre;

    @NotBlank
    @Size(max = 140)
    @Column(length = 140, nullable = false)
    private String slug;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    private String descripcion;

    @NotNull
    @DecimalMin("0.01")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal precioBase;

    /**
     * Precio de venta más bajo entre las variantes activas (ADR 003). Es un dato derivado que la
     * capa de negocio recalcula en cada cambio; permite filtrar y ordenar el catálogo por precio.
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal precioDesde;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private EstadoProducto estado = EstadoProducto.BORRADOR;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductoMaterial> materiales = new ArrayList<>();

    @OneToMany(mappedBy = "producto", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<VarianteProducto> variantes = new ArrayList<>();

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    private List<ImagenProducto> imagenes = new ArrayList<>();

    /** Constructor protegido requerido por JPA. */
    protected Producto() {
    }

    /**
     * Crea un producto en estado {@link EstadoProducto#BORRADOR}.
     *
     * @param sku          SKU único, máximo 40 caracteres
     * @param nombre       nombre visible, máximo 120 caracteres
     * @param slug         identificador único para URLs, máximo 140 caracteres
     * @param descripcion  texto libre (opcional)
     * @param categoria    categoría a la que pertenece
     * @param precioBase   precio base, mayor o igual a 0,01
     */
    public Producto(String sku, String nombre, String slug, String descripcion,
                    Categoria categoria, BigDecimal precioBase) {
        this.sku = Objects.requireNonNull(sku, "sku");
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.slug = Objects.requireNonNull(slug, "slug");
        this.descripcion = descripcion;
        this.categoria = Objects.requireNonNull(categoria, "categoria");
        this.precioBase = Objects.requireNonNull(precioBase, "precioBase");
    }

    /** Cambia los datos editables desde el panel. */
    public void actualizar(String sku, String nombre, String slug, String descripcion,
                           Categoria categoria, BigDecimal precioBase) {
        this.sku = Objects.requireNonNull(sku, "sku");
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.slug = Objects.requireNonNull(slug, "slug");
        this.descripcion = descripcion;
        this.categoria = Objects.requireNonNull(categoria, "categoria");
        this.precioBase = Objects.requireNonNull(precioBase, "precioBase");
    }

    /**
     * Sustituye la lista completa de materiales del producto.
     *
     * @throws IllegalArgumentException si el mismo material aparece más de una vez
     */
    public void reemplazarMateriales(List<ProductoMaterial> nuevos) {
        Objects.requireNonNull(nuevos, "materiales");
        var porMaterial = new java.util.LinkedHashMap<Object, ProductoMaterial>();
        for (var nuevo : nuevos) {
            var material = Objects.requireNonNull(nuevo.getMaterial(), "material");
            Object clave = material.getId() != null ? material.getId() : material;
            if (porMaterial.putIfAbsent(clave, nuevo) != null) {
                throw new IllegalArgumentException("El material " + material.getTipo() + " está repetido en el producto");
            }
        }
        // Se fusiona en lugar de vaciar y volver a añadir: Hibernate inserta antes de borrar, y
        // reinsertar la misma clave compuesta (producto, material) violaría la clave primaria.
        materiales.removeIf(actual -> porMaterial.values().stream()
                .noneMatch(nuevo -> mismoMaterial(actual.getMaterial(), nuevo.getMaterial())));
        for (var nuevo : porMaterial.values()) {
            var existente = materiales.stream()
                    .filter(actual -> mismoMaterial(actual.getMaterial(), nuevo.getMaterial()))
                    .findFirst();
            if (existente.isPresent()) {
                existente.get().actualizar(nuevo.getRol(), nuevo.getPesoGramos(), nuevo.getQuilates());
            } else {
                nuevo.asignarProducto(this);
                materiales.add(nuevo);
            }
        }
    }

    private static boolean mismoMaterial(Material a, Material b) {
        return a == b || (a.getId() != null && a.getId().equals(b.getId()));
    }

    public void agregarVariante(VarianteProducto variante) {
        Objects.requireNonNull(variante, "variante").asignarProducto(this);
        variantes.add(variante);
    }

    /** Añade una imagen; la primera del producto queda como principal. */
    public void agregarImagen(ImagenProducto imagen) {
        Objects.requireNonNull(imagen, "imagen").asignarProducto(this);
        if (imagenes.isEmpty()) {
            imagen.marcarPrincipal();
        } else {
            imagen.desmarcarPrincipal();
        }
        imagenes.add(imagen);
    }

    /** Marca una imagen como principal y desmarca el resto. */
    public void marcarImagenPrincipal(ImagenProducto imagen) {
        Objects.requireNonNull(imagen, "imagen");
        for (var actual : imagenes) {
            if (actual == imagen) {
                actual.marcarPrincipal();
            } else {
                actual.desmarcarPrincipal();
            }
        }
    }

    /** Quita una imagen; si era la principal, pasa a serlo la de menor orden. */
    public void quitarImagen(ImagenProducto imagen) {
        Objects.requireNonNull(imagen, "imagen");
        boolean eraPrincipal = imagen.isPrincipal();
        if (!imagenes.remove(imagen)) {
            return;
        }
        if (eraPrincipal && !imagenes.isEmpty()) {
            ImagenProducto siguiente = imagenes.get(0);
            for (var candidata : imagenes) {
                if (candidata.getOrden() < siguiente.getOrden()) {
                    siguiente = candidata;
                }
            }
            marcarImagenPrincipal(siguiente);
        }
    }

    /** Mensajes en español de todo lo que falta para poder publicar (vacío si no falta nada). */
    public List<String> requisitosPendientesParaPublicar() {
        List<String> pendientes = new ArrayList<>();
        if (categoria == null || !categoria.isActiva()) {
            pendientes.add("La categoría del producto debe estar activa");
        }
        if (materiales.isEmpty()) {
            pendientes.add("El producto debe tener al menos un material");
        }
        if (variantes.stream().noneMatch(VarianteProducto::isActiva)) {
            pendientes.add("El producto debe tener al menos una variante activa");
        }
        if (imagenes.isEmpty()) {
            pendientes.add("El producto debe tener al menos una imagen");
        }
        return List.copyOf(pendientes);
    }

    /**
     * Publica el producto si cumple todos los requisitos.
     *
     * @throws IllegalStateException con la lista de lo que falta
     */
    public void publicar() {
        List<String> pendientes = requisitosPendientesParaPublicar();
        if (!pendientes.isEmpty()) {
            throw new IllegalStateException(String.join(". ", pendientes));
        }
        this.estado = EstadoProducto.PUBLICADO;
    }

    public void archivar() {
        this.estado = EstadoProducto.ARCHIVADO;
    }

    /**
     * Devuelve el producto a borrador.
     *
     * @throws IllegalStateException si ya está en borrador
     */
    public void volverABorrador() {
        if (estado == EstadoProducto.BORRADOR) {
            throw new IllegalStateException("El producto ya está en borrador");
        }
        this.estado = EstadoProducto.BORRADOR;
    }

    public Long getId() {
        return id;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public String getSku() {
        return sku;
    }

    public String getNombre() {
        return nombre;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getPrecioBase() {
        return precioBase;
    }

    public void actualizarPrecioDesde(BigDecimal precioDesde) {
        this.precioDesde = precioDesde;
    }

    public BigDecimal getPrecioDesde() {
        return precioDesde;
    }

    public EstadoProducto getEstado() {
        return estado;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public List<ProductoMaterial> getMateriales() {
        return materiales;
    }

    public List<VarianteProducto> getVariantes() {
        return variantes;
    }

    public List<ImagenProducto> getImagenes() {
        return imagenes;
    }
}
