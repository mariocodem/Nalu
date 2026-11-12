package co.nalu.negocio.clientes;

import co.nalu.datos.entidad.Cliente;
import co.nalu.datos.entidad.Direccion;
import co.nalu.datos.repositorio.DireccionRepository;
import co.nalu.datos.repositorio.UsuarioRepository;
import co.nalu.negocio.clientes.dto.DireccionDto;
import co.nalu.negocio.clientes.dto.DireccionRequest;
import co.nalu.negocio.clientes.dto.PerfilDto;
import co.nalu.negocio.clientes.dto.PerfilRequest;
import co.nalu.negocio.comun.RecursoNoEncontradoException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF02: perfil y direcciones del cliente. Todas las operaciones se filtran por el id del token, así
 * que un recurso ajeno responde 404 (plan §8.5, RNF02).
 */
@Service
public class ClienteService {

    private final UsuarioRepository usuarios;
    private final DireccionRepository direcciones;

    public ClienteService(UsuarioRepository usuarios, DireccionRepository direcciones) {
        this.usuarios = usuarios;
        this.direcciones = direcciones;
    }

    @Transactional(readOnly = true)
    public PerfilDto perfil(long idCliente) {
        return perfil(cliente(idCliente));
    }

    @Transactional
    public PerfilDto actualizarPerfil(long idCliente, PerfilRequest solicitud) {
        var cliente = cliente(idCliente);
        cliente.actualizarPerfil(solicitud.nombre(), solicitud.apellido(), solicitud.telefono(), solicitud.documento(),
                solicitud.aceptaComunicaciones());
        return perfil(cliente);
    }

    @Transactional(readOnly = true)
    public List<DireccionDto> direcciones(long idCliente) {
        return direcciones.findByClienteIdAndActivaTrueOrderByIdAsc(idCliente).stream().map(ClienteService::dto).toList();
    }

    @Transactional
    public DireccionDto crearDireccion(long idCliente, DireccionRequest d) {
        var direccion = new Direccion(cliente(idCliente), d.etiqueta(), d.destinatario(), d.departamento(), d.ciudad(),
                d.direccion(), d.codigoPostal(), d.telefono());
        return dto(direcciones.save(direccion));
    }

    @Transactional
    public DireccionDto actualizarDireccion(long idCliente, long idDireccion, DireccionRequest d) {
        var direccion = direccion(idCliente, idDireccion);
        direccion.actualizar(d.etiqueta(), d.destinatario(), d.departamento(), d.ciudad(), d.direccion(),
                d.codigoPostal(), d.telefono());
        return dto(direccion);
    }

    /** Se desactiva en lugar de borrarse: puede estar referenciada por pedidos (plan §9). */
    @Transactional
    public void eliminarDireccion(long idCliente, long idDireccion) {
        direccion(idCliente, idDireccion).desactivar();
    }

    private Cliente cliente(long id) {
        return usuarios.findById(id).filter(Cliente.class::isInstance).map(Cliente.class::cast)
                .orElseThrow(() -> new RecursoNoEncontradoException("La cuenta de cliente no existe"));
    }

    private Direccion direccion(long idCliente, long idDireccion) {
        return direcciones.findByIdAndClienteIdAndActivaTrue(idDireccion, idCliente)
                .orElseThrow(() -> new RecursoNoEncontradoException("La dirección no existe"));
    }

    private static PerfilDto perfil(Cliente c) {
        return new PerfilDto(c.getNombre(), c.getApellido(), c.getCorreo(), c.getTelefono(), c.getDocumento(),
                c.isAceptaComunicaciones());
    }

    private static DireccionDto dto(Direccion d) {
        return new DireccionDto(d.getId(), d.getEtiqueta(), d.getDestinatario(), d.getDepartamento(), d.getCiudad(),
                d.getDireccion(), d.getCodigoPostal(), d.getTelefono());
    }
}
