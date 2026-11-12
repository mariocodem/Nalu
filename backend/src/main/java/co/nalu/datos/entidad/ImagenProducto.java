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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * Imagen de un producto. Solo una puede ser la principal; el agregado {@link Producto} se encarga
 * de desmarcar las demás (plan de desarrollo §5.5).
 */
@Entity
@Table(name = "imagen_producto")
public class ImagenProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_imagen")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false,
            foreignKey = @ForeignKey(name = "fk_imagen_producto_producto"))
    private Producto producto;

    @NotBlank
    @Size(max = 255)
    @Column(length = 255, nullable = false)
    private String url;

    @NotBlank
    @Size(max = 160)
    @Column(length = 160, nullable = false)
    private String textoAlternativo;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    private boolean principal;

    /** Constructor protegido requerido por JPA. */
    protected ImagenProducto() {
    }

    /**
     * Crea una imagen de producto.
     *
     * @param producto         producto al que pertenece
     * @param url              URL pública de la imagen, máximo 255 caracteres
     * @param textoAlternativo texto alternativo accesible (RNF09), máximo 160 caracteres
     * @param orden            posición dentro de la galería
     */
    public ImagenProducto(Producto producto, String url, String textoAlternativo, int orden) {
        this.producto = Objects.requireNonNull(producto, "producto");
        this.url = Objects.requireNonNull(url, "url");
        this.textoAlternativo = Objects.requireNonNull(textoAlternativo, "textoAlternativo");
        this.orden = orden;
    }

    void asignarProducto(Producto producto) {
        this.producto = Objects.requireNonNull(producto, "producto");
    }

    /** Cambia el texto alternativo y el orden desde el panel. */
    public void actualizar(String textoAlternativo, int orden) {
        this.textoAlternativo = Objects.requireNonNull(textoAlternativo, "textoAlternativo");
        this.orden = orden;
    }

    void marcarPrincipal() {
        this.principal = true;
    }

    void desmarcarPrincipal() {
        this.principal = false;
    }

    public Long getId() {
        return id;
    }

    public Producto getProducto() {
        return producto;
    }

    public String getUrl() {
        return url;
    }

    public String getTextoAlternativo() {
        return textoAlternativo;
    }

    public int getOrden() {
        return orden;
    }

    public boolean isPrincipal() {
        return principal;
    }
}
