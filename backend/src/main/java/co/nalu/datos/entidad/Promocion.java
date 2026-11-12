package co.nalu.datos.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/** Cupón de descuento (RF11). */
@Entity
@Table(name = "promocion", uniqueConstraints = @UniqueConstraint(name = "uk_promocion_codigo", columnNames = "codigo"))
public class Promocion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_promocion")
    private Long id;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPromocion tipo;

    @DecimalMin("0.01")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    /** {@code null}: sin límite de usos. */
    @Min(1)
    private Integer usosMaximos;

    @Min(0)
    @Column(nullable = false)
    private int usosActuales;

    @Column(nullable = false)
    private boolean activa = true;

    @Version
    private long version;

    protected Promocion() {
    }

    public Promocion(String codigo, TipoPromocion tipo, BigDecimal valor, LocalDate fechaInicio, LocalDate fechaFin,
                     Integer usosMaximos) {
        this.codigo = Objects.requireNonNull(codigo, "codigo");
        actualizar(tipo, valor, fechaInicio, fechaFin, usosMaximos, true);
    }

    public void actualizar(TipoPromocion tipo, BigDecimal valor, LocalDate fechaInicio, LocalDate fechaFin,
                           Integer usosMaximos, boolean activa) {
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.valor = Objects.requireNonNull(valor, "valor");
        this.fechaInicio = Objects.requireNonNull(fechaInicio, "fechaInicio");
        this.fechaFin = Objects.requireNonNull(fechaFin, "fechaFin");
        this.usosMaximos = usosMaximos;
        this.activa = activa;
    }

    /** Motivo por el que no se puede usar hoy, o {@code null} si es válida. */
    public String motivoNoAplicable(LocalDate hoy) {
        if (!activa) {
            return "El cupón no está activo";
        }
        if (hoy.isBefore(fechaInicio)) {
            return "El cupón todavía no está vigente";
        }
        if (hoy.isAfter(fechaFin)) {
            return "El cupón expiró";
        }
        if (usosMaximos != null && usosActuales >= usosMaximos) {
            return "El cupón ya alcanzó su límite de usos";
        }
        return null;
    }

    /** Descuento en pesos enteros, nunca mayor que el subtotal. */
    public BigDecimal descuentoPara(BigDecimal subtotal) {
        var descuento = tipo == TipoPromocion.PORCENTAJE
                ? subtotal.multiply(valor).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                : valor.setScale(0, RoundingMode.HALF_UP);
        return descuento.min(subtotal).setScale(2, RoundingMode.UNNECESSARY);
    }

    public void registrarUso() {
        usosActuales++;
    }

    @AssertTrue(message = "La fecha final debe ser igual o posterior a la inicial")
    private boolean isRangoValido() {
        return fechaInicio == null || fechaFin == null || !fechaFin.isBefore(fechaInicio);
    }

    @AssertTrue(message = "Un porcentaje no puede superar 100")
    private boolean isPorcentajeValido() {
        return tipo != TipoPromocion.PORCENTAJE || valor == null || valor.compareTo(BigDecimal.valueOf(100)) <= 0;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public TipoPromocion getTipo() { return tipo; }
    public BigDecimal getValor() { return valor; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public Integer getUsosMaximos() { return usosMaximos; }
    public int getUsosActuales() { return usosActuales; }
    public boolean isActiva() { return activa; }
}
