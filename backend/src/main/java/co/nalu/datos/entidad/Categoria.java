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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Categoría del catálogo, organizada como árbol mediante una relación reflexiva.
 *
 * <p>Una categoría solo puede colgar de otra que no sea ella misma ni una de sus descendientes
 * (evita ciclos, contrato de la API §4).
 */
@Entity
@Table(name = "categoria",
        uniqueConstraints = @UniqueConstraint(name = "uk_categoria_slug", columnNames = "slug"))
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria_padre", foreignKey = @ForeignKey(name = "fk_categoria_categoria"))
    private Categoria padre;

    @OneToMany(mappedBy = "padre")
    private List<Categoria> hijos = new ArrayList<>();

    @NotBlank
    @Size(max = 80)
    @Column(length = 80, nullable = false)
    private String nombre;

    @NotBlank
    @Size(max = 100)
    @Column(length = 100, nullable = false)
    private String slug;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    private String descripcion;

    @Column(nullable = false)
    private boolean activa = true;

    /** Constructor protegido requerido por JPA. */
    protected Categoria() {
    }

    /**
     * Crea una categoría.
     *
     * @param nombre      nombre visible, máximo 80 caracteres
     * @param slug        identificador único para URLs, máximo 100 caracteres
     * @param descripcion texto libre (opcional)
     * @param padre       categoría padre, o {@code null} si es una raíz
     */
    public Categoria(String nombre, String slug, String descripcion, Categoria padre) {
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.slug = Objects.requireNonNull(slug, "slug");
        this.descripcion = descripcion;
        this.padre = padre;
    }

    /** Cambia los datos editables desde el panel. */
    public void actualizar(String nombre, String slug, String descripcion) {
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.slug = Objects.requireNonNull(slug, "slug");
        this.descripcion = descripcion;
    }

    /**
     * Mueve la categoría bajo otro padre.
     *
     * @throws IllegalArgumentException si el nuevo padre es la propia categoría o una descendiente
     *                                  (crearía un ciclo)
     */
    public void moverA(Categoria nuevoPadre) {
        for (Categoria actual = nuevoPadre; actual != null; actual = actual.padre) {
            if (actual == this || (id != null && id.equals(actual.id))) {
                throw new IllegalArgumentException(
                        "Una categoría no puede depender de sí misma ni de una descendiente");
            }
        }
        this.padre = nuevoPadre;
    }

    public void activar() {
        this.activa = true;
    }

    public void desactivar() {
        this.activa = false;
    }

    public Long getId() {
        return id;
    }

    public Categoria getPadre() {
        return padre;
    }

    public List<Categoria> getHijos() {
        return hijos;
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

    public boolean isActiva() {
        return activa;
    }
}
