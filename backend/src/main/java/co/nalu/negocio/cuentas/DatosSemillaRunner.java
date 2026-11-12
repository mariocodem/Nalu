package co.nalu.negocio.cuentas;

import co.nalu.config.AdministradorInicialProperties;
import co.nalu.datos.entidad.Administrador;
import co.nalu.datos.entidad.Categoria;
import co.nalu.datos.entidad.Material;
import co.nalu.datos.repositorio.CategoriaRepository;
import co.nalu.datos.repositorio.MaterialRepository;
import co.nalu.negocio.comun.Slugs;
import java.math.BigDecimal;
import java.util.List;
import co.nalu.datos.repositorio.UsuarioRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Datos semilla de los perfiles {@code dev} y {@code demo} (plan §5.6). Es idempotente: solo crea lo
 * que no existe.
 *
 * <p>Crea el administrador inicial, los materiales (factor de precio 1 hasta que se decida la
 * fórmula, CLAUDE.md §12) y las categorías raíz del plan §5.6.
 */
@Component
@Profile({"dev", "demo"})
public class DatosSemillaRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosSemillaRunner.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final AdministradorInicialProperties admin;
    private final MaterialRepository materiales;
    private final CategoriaRepository categorias;

    /** Tipo y ley de los materiales iniciales (plan §5.6). */
    static final List<String[]> MATERIALES = List.of(new String[] {"Plata", "925"}, new String[] {"Acero", "316L"},
            new String[] {"Oro", "18K"}, new String[] {"Esmeralda", null}, new String[] {"Cuarzo", null});
    static final List<String> CATEGORIAS = List.of("Anillos", "Collares", "Aretes", "Pulseras");

    public DatosSemillaRunner(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
                              AdministradorInicialProperties admin, MaterialRepository materiales,
                              CategoriaRepository categorias) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.admin = admin;
        this.materiales = materiales;
        this.categorias = categorias;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        crearAdministrador();
        for (var m : MATERIALES) {
            boolean existe = m[1] == null ? materiales.existsByTipoIgnoreCaseAndLeyIsNull(m[0])
                    : materiales.existsByTipoIgnoreCaseAndLey(m[0], m[1]);
            if (!existe) {
                materiales.save(new Material(m[0], m[1], false, BigDecimal.ONE));
            }
        }
        for (var nombre : CATEGORIAS) {
            var slug = Slugs.generar(nombre);
            if (!categorias.existsBySlug(slug)) {
                categorias.save(new Categoria(nombre, slug, null, null));
            }
        }
    }

    private void crearAdministrador() {
        var correo = admin.correo().strip().toLowerCase(Locale.ROOT);
        if (usuarios.existsByCorreo(correo)) {
            return;
        }
        usuarios.save(new Administrador("Administrador", "Nalu", correo, passwordEncoder.encode(admin.contrasena())));
        log.info("Administrador inicial creado: {}", correo);
    }
}
