package co.nalu.datos.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Evento recibido de la pasarela. {@code claveIdempotencia} ({@code idTransaccion:estado}) es
 * única: un evento repetido no se procesa dos veces (plan §6.4, marco conceptual 11.2).
 */
@Entity
@Table(name = "evento_pasarela",
        uniqueConstraints = @UniqueConstraint(name = "uk_evento_pasarela_clave", columnNames = "clave_idempotencia"))
public class EventoPasarela {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Long id;

    @Column(nullable = false, length = 120)
    private String claveIdempotencia;

    @Column(nullable = false, length = 60)
    private String tipo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> carga;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime fechaRecepcion;

    protected EventoPasarela() {
    }

    public EventoPasarela(String claveIdempotencia, String tipo, Map<String, Object> carga) {
        this.claveIdempotencia = Objects.requireNonNull(claveIdempotencia, "claveIdempotencia");
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.carga = Objects.requireNonNull(carga, "carga");
    }

    public Long getId() { return id; }
    public String getClaveIdempotencia() { return claveIdempotencia; }
    public String getTipo() { return tipo; }
    public Map<String, Object> getCarga() { return carga; }
    public OffsetDateTime getFechaRecepcion() { return fechaRecepcion; }
}
