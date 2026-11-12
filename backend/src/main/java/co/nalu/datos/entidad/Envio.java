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
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

/** Despacho de un pedido (RF14, CU-08/CU-13): transportadora, guía y entrega. */
@Entity
@Table(name = "envio", uniqueConstraints = @UniqueConstraint(name = "uk_envio_pedido", columnNames = "id_pedido"))
public class Envio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_envio")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false, foreignKey = @ForeignKey(name = "fk_envio_pedido"))
    private Pedido pedido;

    @Column(nullable = false, length = 60)
    private String transportadora;

    @Column(nullable = false, length = 60)
    private String numeroGuia;

    /** ENVIADO o ENTREGADO, como el pedido. */
    @Column(nullable = false, length = 25)
    private String estado = "ENVIADO";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal costo;

    @Column(nullable = false)
    private OffsetDateTime fechaDespacho;

    private OffsetDateTime fechaEntrega;

    protected Envio() {
    }

    public Envio(Pedido pedido, String transportadora, String numeroGuia, BigDecimal costo, OffsetDateTime fechaDespacho) {
        this.pedido = Objects.requireNonNull(pedido, "pedido");
        this.transportadora = Objects.requireNonNull(transportadora, "transportadora");
        this.numeroGuia = Objects.requireNonNull(numeroGuia, "numeroGuia");
        this.costo = Objects.requireNonNull(costo, "costo");
        this.fechaDespacho = Objects.requireNonNull(fechaDespacho, "fechaDespacho");
    }

    public void registrarEntrega(OffsetDateTime cuando) {
        this.estado = "ENTREGADO";
        this.fechaEntrega = Objects.requireNonNull(cuando, "cuando");
    }

    public Long getId() { return id; }
    public Pedido getPedido() { return pedido; }
    public String getTransportadora() { return transportadora; }
    public String getNumeroGuia() { return numeroGuia; }
    public String getEstado() { return estado; }
    public BigDecimal getCosto() { return costo; }
    public OffsetDateTime getFechaDespacho() { return fechaDespacho; }
    public OffsetDateTime getFechaEntrega() { return fechaEntrega; }
}
