package co.nalu.negocio.auditoria;

import co.nalu.datos.entidad.Auditoria;
import co.nalu.datos.repositorio.AuditoriaRepository;
import co.nalu.datos.repositorio.UsuarioRepository;
import co.nalu.negocio.comun.Pagina;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** RNF15: trazabilidad de las acciones del panel y de los cambios de precio. */
@Service
public class AuditoriaService {

    public record Registro(Long id, OffsetDateTime fecha, String usuario, String accion, String recurso, String idRecurso,
                           String detalle) {}

    private final AuditoriaRepository auditoria;
    private final UsuarioRepository usuarios;

    public AuditoriaService(AuditoriaRepository auditoria, UsuarioRepository usuarios) {
        this.auditoria = auditoria;
        this.usuarios = usuarios;
    }

    /**
     * En su propia transacción: una acción del panel queda registrada aunque la petición haya
     * fallado (el estado HTTP va en el detalle).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarAccion(Long idUsuario, String accion, String recurso, String idRecurso, String detalle) {
        var usuario = idUsuario == null ? null : usuarios.findById(idUsuario).orElse(null);
        auditoria.save(new Auditoria(usuario, accion, recurso, idRecurso, detalle));
    }

    /** Dentro de la transacción del cambio: si el cambio se revierte, el registro también. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarCambio(String accion, String recurso, Object idRecurso, String detalle) {
        auditoria.save(new Auditoria(null, accion, recurso, String.valueOf(idRecurso), detalle));
    }

    @Transactional(readOnly = true)
    public Pagina<Registro> listar(String recurso, String idRecurso, Pageable pagina) {
        var resultado = recurso == null || idRecurso == null ? auditoria.findAll(pagina)
                : auditoria.findByRecursoAndIdRecurso(recurso, idRecurso, pagina);
        return Pagina.de(resultado, a -> new Registro(a.getId(), a.getFecha(),
                a.getUsuario() == null ? "Sistema" : a.getUsuario().getCorreo(), a.getAccion(), a.getRecurso(),
                a.getIdRecurso(), a.getDetalle()));
    }
}
