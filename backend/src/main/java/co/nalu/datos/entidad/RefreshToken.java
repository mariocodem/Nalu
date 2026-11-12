package co.nalu.datos.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Refresh token opaco emitido tras un inicio de sesión.
 *
 * <p>Solo se guarda el hash SHA-256 en hexadecimal del valor entregado al cliente; el valor en
 * claro nunca se persiste. Los tokens de un mismo login comparten {@code familia} para poder
 * revocar toda la cadena si se detecta reutilización (plan de desarrollo §8.3).
 *
 * <p>Cada uso rota el token: el presentado se revoca y se emite otro de la misma familia. Por eso
 * la expiración por inactividad se fija al crear cada token ({@code creadoEn + inactividad}) y el
 * tope absoluto de la sesión ({@code limiteAbsoluto}) se fija en el login y se hereda en cada
 * rotación; si se calculara desde {@code creadoEn}, se reiniciaría con cada refresh.
 *
 * <p>La detección de robo (revocar toda la familia) vive en la capa de negocio.
 */
@Entity
@Table(name = "refresh_token",
        uniqueConstraints = @UniqueConstraint(name = "uk_refresh_token_hash", columnNames = "hash_token"),
        indexes = {
                @Index(name = "idx_refresh_token_familia", columnList = "familia"),
                @Index(name = "idx_refresh_token_usuario", columnList = "id_usuario")
        })
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_refresh_token")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, foreignKey = @ForeignKey(name = "fk_refresh_token_usuario"))
    private Usuario usuario;

    @Column(nullable = false, length = 64)
    private String hashToken;

    @Column(nullable = false)
    private UUID familia;

    @Column(nullable = false)
    private OffsetDateTime creadoEn;

    @Column(nullable = false)
    private OffsetDateTime expiraEn;

    /** Instante a partir del cual ningún token de la familia es válido (login + vida máxima). */
    @Column(nullable = false)
    private OffsetDateTime limiteAbsoluto;

    private OffsetDateTime revocadoEn;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reemplazado_por", foreignKey = @ForeignKey(name = "fk_refresh_token_reemplazado_por"))
    private RefreshToken reemplazadoPor;

    @Size(max = 45)
    @Column(length = 45)
    private String ip;

    @Size(max = 255)
    @Column(length = 255)
    private String userAgent;

    protected RefreshToken() {
    }

    private RefreshToken(Usuario usuario, String hashToken, UUID familia, OffsetDateTime creadoEn,
                         Duration inactividad, OffsetDateTime limiteAbsoluto, String ip, String userAgent) {
        this.usuario = Objects.requireNonNull(usuario, "usuario");
        this.hashToken = Objects.requireNonNull(hashToken, "hashToken");
        this.familia = Objects.requireNonNull(familia, "familia");
        this.creadoEn = Objects.requireNonNull(creadoEn, "creadoEn");
        this.limiteAbsoluto = Objects.requireNonNull(limiteAbsoluto, "limiteAbsoluto");
        OffsetDateTime porInactividad = creadoEn.plus(Objects.requireNonNull(inactividad, "inactividad"));
        this.expiraEn = porInactividad.isBefore(limiteAbsoluto) ? porInactividad : limiteAbsoluto;
        this.ip = ip;
        this.userAgent = userAgent;
    }

    /** Primer token de una sesión nueva (login): abre una familia y fija su tope absoluto. */
    public static RefreshToken iniciarFamilia(Usuario usuario, String hashToken, OffsetDateTime ahora,
                                              Duration inactividad, Duration vidaMaxima,
                                              String ip, String userAgent) {
        Objects.requireNonNull(vidaMaxima, "vidaMaxima");
        return new RefreshToken(usuario, hashToken, UUID.randomUUID(), ahora, inactividad,
                ahora.plus(vidaMaxima), ip, userAgent);
    }

    /**
     * Revoca este token y devuelve su sucesor en la misma familia, que hereda el tope absoluto.
     * Comprobar antes con {@link #estaVigente(OffsetDateTime)} que el token puede rotarse.
     */
    public RefreshToken rotar(String hashNuevo, OffsetDateTime ahora, Duration inactividad,
                              String ip, String userAgent) {
        var sucesor = new RefreshToken(usuario, hashNuevo, familia, ahora, inactividad, limiteAbsoluto,
                ip, userAgent);
        revocar(ahora);
        this.reemplazadoPor = sucesor;
        return sucesor;
    }

    public boolean estaRevocado() {
        return revocadoEn != null;
    }

    public boolean estaVigente(OffsetDateTime ahora) {
        Objects.requireNonNull(ahora, "ahora");
        return !estaRevocado() && ahora.isBefore(expiraEn);
    }

    /** Idempotente: una segunda revocación conserva la fecha de la primera. */
    public void revocar(OffsetDateTime cuando) {
        Objects.requireNonNull(cuando, "cuando");
        if (revocadoEn == null) {
            revocadoEn = cuando;
        }
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getHashToken() {
        return hashToken;
    }

    public UUID getFamilia() {
        return familia;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public OffsetDateTime getExpiraEn() {
        return expiraEn;
    }

    public OffsetDateTime getLimiteAbsoluto() {
        return limiteAbsoluto;
    }

    public OffsetDateTime getRevocadoEn() {
        return revocadoEn;
    }

    public RefreshToken getReemplazadoPor() {
        return reemplazadoPor;
    }

    public String getIp() {
        return ip;
    }

    public String getUserAgent() {
        return userAgent;
    }
}
