package co.nalu.negocio.cuentas;

import co.nalu.config.JwtProperties;
import co.nalu.datos.entidad.Administrador;
import co.nalu.datos.entidad.Usuario;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Emite access tokens JWT y genera los valores opacos de los refresh tokens (plan §8.1). */
@Service
public class TokenService {

    static final String ROL_CLIENTE = "CLIENTE";
    static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";

    private static final int BYTES_REFRESH = 32; // 256 bits

    private final JwtEncoder jwtEncoder;
    private final JwtProperties props;
    private final SecureRandom aleatorio = new SecureRandom();

    public TokenService(JwtEncoder jwtEncoder, JwtProperties props) {
        this.jwtEncoder = jwtEncoder;
        this.props = props;
    }

    /** Sin datos personales sensibles: el token se puede decodificar en el navegador. */
    public Jwt emitirAccessToken(Usuario usuario, Instant ahora) {
        var claims = JwtClaimsSet.builder()
                .issuer(props.emisor())
                .subject(usuario.getId().toString())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(props.vidaAccessToken()))
                .id(UUID.randomUUID().toString())
                .claim("roles", roles(usuario))
                .claim("nombre", usuario.getNombre())
                .build();
        var cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(cabecera, claims));
    }

    public List<String> roles(Usuario usuario) {
        return List.of(usuario instanceof Administrador ? ROL_ADMINISTRADOR : ROL_CLIENTE);
    }

    /** Valor opaco de 256 bits en Base64 URL, sin relleno. Se entrega al cliente y no se guarda. */
    public String nuevoRefreshToken() {
        var bytes = new byte[BYTES_REFRESH];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 en hexadecimal: lo único que se guarda del refresh token. */
    public String hash(String refreshToken) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en la JVM", e);
        }
    }
}
