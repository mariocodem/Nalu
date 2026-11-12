package co.nalu.negocio.cuentas;

import co.nalu.config.JwtProperties;
import co.nalu.datos.entidad.Administrador;
import co.nalu.datos.entidad.Cliente;
import co.nalu.datos.entidad.EstadoUsuario;
import co.nalu.datos.entidad.RefreshToken;
import co.nalu.datos.entidad.Usuario;
import co.nalu.datos.repositorio.RefreshTokenRepository;
import co.nalu.datos.repositorio.UsuarioRepository;
import co.nalu.negocio.comun.ConflictoException;
import co.nalu.negocio.comun.DemasiadosIntentosException;
import co.nalu.negocio.comun.NoAutenticadoException;
import co.nalu.negocio.cuentas.dto.LoginRequest;
import co.nalu.negocio.cuentas.dto.RegistroRequest;
import co.nalu.negocio.cuentas.dto.SesionDto;
import co.nalu.negocio.cuentas.dto.UsuarioSesionDto;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inicio de sesión, renovación y cierre (plan de desarrollo §8.2 y §8.3).
 *
 * <p>Reglas del refresh token: cada uso lo rota; si llega uno ya revocado se asume robo y se revoca
 * toda su familia; el cierre de sesión revoca la familia completa.
 */
@Service
public class AutenticacionService {

    private static final Logger log = LoggerFactory.getLogger(AutenticacionService.class);

    static final String CREDENCIALES_INCORRECTAS = "Correo o contraseña incorrectos";
    static final String CUENTA_BLOQUEADA = "Tu cuenta está bloqueada. Escríbenos para recuperarla.";
    static final String CORREO_REGISTRADO = "Ya existe una cuenta con ese correo";
    static final String SESION_INVALIDA = "Tu sesión expiró. Inicia sesión de nuevo.";

    private final UsuarioRepository usuarios;
    private final RefreshTokenRepository refreshTokens;
    private final TokenService tokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties props;
    private final Clock reloj;
    private final ProteccionFuerzaBruta fuerzaBruta;
    /** Se compara contra él cuando el correo no existe, para que la respuesta tarde lo mismo. */
    private final String hashFicticio;

    public AutenticacionService(UsuarioRepository usuarios, RefreshTokenRepository refreshTokens,
                                TokenService tokens, PasswordEncoder passwordEncoder, JwtProperties props,
                                Clock reloj, ProteccionFuerzaBruta fuerzaBruta) {
        this.usuarios = usuarios;
        this.refreshTokens = refreshTokens;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
        this.reloj = reloj;
        this.fuerzaBruta = fuerzaBruta;
        this.hashFicticio = passwordEncoder.encode("contrasena-ficticia-para-igualar-tiempos");
    }

    /**
     * No revierte la transacción al rechazar: el intento fallido debe quedar guardado para el
     * bloqueo por fuerza bruta.
     */
    @Transactional(noRollbackFor = {NoAutenticadoException.class, DemasiadosIntentosException.class})
    public SesionEmitida iniciarSesion(LoginRequest solicitud, String ip, String userAgent) {
        var correo = solicitud.correo().strip().toLowerCase(Locale.ROOT);
        var ahora = OffsetDateTime.now(reloj);
        fuerzaBruta.verificarQueNoEsteBloqueado(correo, ahora);

        var usuario = usuarios.findByCorreo(correo).orElse(null);
        boolean contrasenaCorrecta;
        if (usuario == null) {
            passwordEncoder.matches(solicitud.contrasena(), hashFicticio); // iguala el tiempo de respuesta
            contrasenaCorrecta = false;
        } else {
            contrasenaCorrecta = passwordEncoder.matches(solicitud.contrasena(), usuario.getContrasenaHash());
        }
        fuerzaBruta.registrar(correo, contrasenaCorrecta, recortar(ip, 45), ahora);
        if (!contrasenaCorrecta) {
            throw new NoAutenticadoException(CREDENCIALES_INCORRECTAS);
        }
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new NoAutenticadoException(CUENTA_BLOQUEADA);
        }
        if (usuario instanceof Administrador administrador) {
            administrador.registrarAcceso(ahora);
        }
        return abrirSesion(usuario, ahora, ip, userAgent);
    }

    /** RF01: crea el cliente y deja la sesión iniciada. El correo es único (409 si ya existe). */
    @Transactional
    public SesionEmitida registrar(RegistroRequest solicitud, String ip, String userAgent) {
        var correo = solicitud.correo().toLowerCase(Locale.ROOT);
        if (usuarios.existsByCorreo(correo)) {
            throw new ConflictoException(CORREO_REGISTRADO);
        }
        var ahora = OffsetDateTime.now(reloj);
        var cliente = new Cliente(solicitud.nombre(), solicitud.apellido(), correo, solicitud.telefono(),
                passwordEncoder.encode(solicitud.contrasena()), null, solicitud.aceptaComunicaciones(), ahora);
        try {
            usuarios.saveAndFlush(cliente);
        } catch (DataIntegrityViolationException e) {
            // Dos registros simultáneos con el mismo correo: lo resuelve la restricción única.
            throw new ConflictoException(CORREO_REGISTRADO);
        }
        return abrirSesion(cliente, ahora, ip, userAgent);
    }

    private SesionEmitida abrirSesion(Usuario usuario, OffsetDateTime ahora, String ip, String userAgent) {
        var valor = tokens.nuevoRefreshToken();
        var refresh = refreshTokens.save(RefreshToken.iniciarFamilia(usuario, tokens.hash(valor), ahora,
                props.inactividadRefresh(), props.vidaMaximaRefresh(), recortar(ip, 45), recortar(userAgent, 255)));
        return emitir(usuario, valor, refresh, ahora);
    }

    /**
     * No revierte la transacción al rechazar: la revocación de la familia por reutilización debe
     * quedar guardada aunque la respuesta sea 401.
     */
    @Transactional(noRollbackFor = NoAutenticadoException.class)
    public SesionEmitida refrescar(String valor, String ip, String userAgent) {
        if (valor == null || valor.isBlank()) {
            throw new NoAutenticadoException(SESION_INVALIDA);
        }
        var presentado = refreshTokens.findByHashToken(tokens.hash(valor))
                .orElseThrow(() -> new NoAutenticadoException(SESION_INVALIDA));
        var ahora = OffsetDateTime.now(reloj);
        if (presentado.estaRevocado()) {
            int revocados = refreshTokens.revocarFamilia(presentado.getFamilia(), ahora);
            log.warn("Reutilización de un refresh token revocado: se revoca la familia {} ({} tokens activos)",
                    presentado.getFamilia(), revocados);
            throw new NoAutenticadoException(SESION_INVALIDA);
        }
        if (!presentado.estaVigente(ahora)) {
            throw new NoAutenticadoException(SESION_INVALIDA);
        }
        var usuario = presentado.getUsuario();
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            refreshTokens.revocarFamilia(presentado.getFamilia(), ahora);
            throw new NoAutenticadoException(CUENTA_BLOQUEADA);
        }
        var valorNuevo = tokens.nuevoRefreshToken();
        var sucesor = refreshTokens.save(presentado.rotar(tokens.hash(valorNuevo), ahora,
                props.inactividadRefresh(), recortar(ip, 45), recortar(userAgent, 255)));
        return emitir(usuario, valorNuevo, sucesor, ahora);
    }

    /** Revoca toda la sesión (la familia del token). Un token desconocido no es un error. */
    @Transactional
    public void cerrarSesion(String valor) {
        if (valor == null || valor.isBlank()) {
            return;
        }
        refreshTokens.findByHashToken(tokens.hash(valor))
                .ifPresent(token -> refreshTokens.revocarFamilia(token.getFamilia(), OffsetDateTime.now(reloj)));
    }

    @Transactional(readOnly = true)
    public UsuarioSesionDto usuarioActual(long idUsuario) {
        return usuarios.findById(idUsuario)
                .filter(usuario -> usuario.getEstado() == EstadoUsuario.ACTIVO)
                .map(this::aDto)
                .orElseThrow(() -> new NoAutenticadoException(SESION_INVALIDA));
    }

    private SesionEmitida emitir(Usuario usuario, String valorRefresh, RefreshToken refresh, OffsetDateTime ahora) {
        var accessToken = tokens.emitirAccessToken(usuario, ahora.toInstant());
        var sesion = new SesionDto(accessToken.getTokenValue(), accessToken.getExpiresAt(), aDto(usuario));
        return new SesionEmitida(sesion, valorRefresh, Duration.between(ahora, refresh.getExpiraEn()));
    }

    private UsuarioSesionDto aDto(Usuario usuario) {
        return new UsuarioSesionDto(usuario.getId(), usuario.getNombre(), usuario.getApellido(),
                usuario.getCorreo(), tokens.roles(usuario));
    }

    private static String recortar(String valor, int maximo) {
        return valor == null || valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }
}
