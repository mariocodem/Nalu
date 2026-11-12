package co.nalu.datos.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Material de una joya (plata, oro, esmeralda, acero…).
 *
 * <p>La combinación {@code tipo + ley} es única; la ley es opcional porque los materiales no
 * metálicos (piedras) no la tienen.
 */
@Entity
@Table(name = "material",
        uniqueConstraints = @UniqueConstraint(name = "uk_material_tipo_ley",
                columnNames = {"tipo", "ley"}))
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_material")
    private Long id;

    @NotBlank
    @Size(max = 30)
    @Column(length = 30, nullable = false)
    private String tipo;

    @Size(max = 20)
    @Column(length = 20)
    private String ley;

    @Column(nullable = false)
    private boolean requiereCertificado;

    @NotNull
    @DecimalMin("0.001")
    @Column(precision = 6, scale = 3, nullable = false)
    private BigDecimal factorPrecio;

    /** Constructor protegido requerido por JPA. */
    protected Material() {
    }

    /**
     * Crea un material.
     *
     * @param tipo                 nombre del material, máximo 30 caracteres
     * @param ley                  ley o pureza (opcional), máximo 20 caracteres
     * @param requiereCertificado  si exige certificado de autenticidad
     * @param factorPrecio         multiplicador de precio, mayor o igual a 0,001
     */
    public Material(String tipo, String ley, boolean requiereCertificado, BigDecimal factorPrecio) {
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.ley = ley;
        this.requiereCertificado = requiereCertificado;
        this.factorPrecio = Objects.requireNonNull(factorPrecio, "factorPrecio");
    }

    /** Cambia los datos editables desde el panel. */
    public void actualizar(String tipo, String ley, boolean requiereCertificado, BigDecimal factorPrecio) {
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.ley = ley;
        this.requiereCertificado = requiereCertificado;
        this.factorPrecio = Objects.requireNonNull(factorPrecio, "factorPrecio");
    }

    public Long getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getLey() {
        return ley;
    }

    public boolean isRequiereCertificado() {
        return requiereCertificado;
    }

    public BigDecimal getFactorPrecio() {
        return factorPrecio;
    }
}
