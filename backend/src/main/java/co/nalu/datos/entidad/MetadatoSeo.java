package co.nalu.datos.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Metadatos SEO de un producto o de una categoría (RF05, plan §5.3). Los campos vacíos se
 * completan con valores derivados del producto o la categoría al publicarlos.
 */
@Entity
@Table(name = "metadato_seo", uniqueConstraints = {
        @UniqueConstraint(name = "uk_metadato_seo_producto", columnNames = "id_producto"),
        @UniqueConstraint(name = "uk_metadato_seo_categoria", columnNames = "id_categoria")
})
public class MetadatoSeo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_metadato")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", foreignKey = @ForeignKey(name = "fk_metadato_seo_producto"))
    private Producto producto;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", foreignKey = @ForeignKey(name = "fk_metadato_seo_categoria"))
    private Categoria categoria;

    @Size(max = 160)
    @Column(length = 160)
    private String titulo;

    @Size(max = 320)
    @Column(length = 320)
    private String metaDescripcion;

    @Size(max = 255)
    @Column(length = 255)
    private String palabrasClave;

    @Size(max = 255)
    @Column(length = 255)
    private String urlCanonica;

    /** Propiedades Schema.org adicionales que se mezclan con las generadas (p. ej. {@code brand}). */
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> datosEstructurados;

    protected MetadatoSeo() {
    }

    public static MetadatoSeo deProducto(Producto producto) {
        var m = new MetadatoSeo();
        m.producto = producto;
        return m;
    }

    public static MetadatoSeo deCategoria(Categoria categoria) {
        var m = new MetadatoSeo();
        m.categoria = categoria;
        return m;
    }

    public void actualizar(String titulo, String metaDescripcion, String palabrasClave, String urlCanonica,
                           Map<String, Object> datosEstructurados) {
        this.titulo = titulo;
        this.metaDescripcion = metaDescripcion;
        this.palabrasClave = palabrasClave;
        this.urlCanonica = urlCanonica;
        this.datosEstructurados = datosEstructurados == null || datosEstructurados.isEmpty() ? null : Map.copyOf(datosEstructurados);
    }

    @AssertTrue(message = "El metadato debe pertenecer a un producto o a una categoría, no a ambos")
    private boolean isDuenoUnico() {
        return (producto == null) != (categoria == null);
    }

    public Long getId() {
        return id;
    }

    public Producto getProducto() {
        return producto;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMetaDescripcion() {
        return metaDescripcion;
    }

    public String getPalabrasClave() {
        return palabrasClave;
    }

    public String getUrlCanonica() {
        return urlCanonica;
    }

    public Map<String, Object> getDatosEstructurados() {
        return datosEstructurados;
    }
}
