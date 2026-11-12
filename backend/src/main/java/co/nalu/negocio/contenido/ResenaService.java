package co.nalu.negocio.contenido;

import co.nalu.datos.entidad.Cliente;
import co.nalu.datos.entidad.EstadoProducto;
import co.nalu.datos.entidad.EstadoResena;
import co.nalu.datos.entidad.Resena;
import co.nalu.datos.repositorio.PedidoRepository;
import co.nalu.datos.repositorio.ProductoRepository;
import co.nalu.datos.repositorio.ResenaRepository;
import co.nalu.datos.repositorio.UsuarioRepository;
import co.nalu.negocio.comun.ConflictoException;
import co.nalu.negocio.comun.Pagina;
import co.nalu.negocio.comun.RecursoNoEncontradoException;
import co.nalu.negocio.contenido.dto.ResenaDtos.ResenaAdmin;
import co.nalu.negocio.contenido.dto.ResenaDtos.ResenaPublica;
import co.nalu.negocio.contenido.dto.ResenaDtos.ResenaRequest;
import co.nalu.negocio.contenido.dto.ResenaDtos.Resumen;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF07: reseñas de quien recibió el producto, publicadas tras la aprobación del administrador. */
@Service
public class ResenaService {

    private final ResenaRepository resenas;
    private final ProductoRepository productos;
    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;

    public ResenaService(ResenaRepository resenas, ProductoRepository productos, PedidoRepository pedidos,
                         UsuarioRepository usuarios) {
        this.resenas = resenas;
        this.productos = productos;
        this.pedidos = pedidos;
        this.usuarios = usuarios;
    }

    @Transactional
    public void crear(long idCliente, String slug, ResenaRequest solicitud) {
        var producto = productos.findBySlugAndEstado(slug, EstadoProducto.PUBLICADO)
                .orElseThrow(() -> new RecursoNoEncontradoException("El producto no existe"));
        if (!pedidos.clienteRecibioProducto(idCliente, producto.getId())) {
            throw new ConflictoException("Solo puedes reseñar piezas que ya recibiste");
        }
        if (resenas.existsByProductoIdAndClienteId(producto.getId(), idCliente)) {
            throw new ConflictoException("Ya escribiste una reseña de esta pieza");
        }
        var cliente = usuarios.findById(idCliente).filter(Cliente.class::isInstance).map(Cliente.class::cast).orElseThrow();
        resenas.save(new Resena(producto, cliente, solicitud.calificacion(), solicitud.comentario()));
    }

    /** Para la ficha: solo las aprobadas. */
    @Transactional(readOnly = true)
    public Resumen publicas(long idProducto) {
        var aprobadas = resenas.findByProductoIdAndEstadoOrderByFechaDesc(idProducto, EstadoResena.APROBADA);
        var promedio = aprobadas.isEmpty() ? null
                : Math.round(aprobadas.stream().mapToInt(Resena::getCalificacion).average().orElse(0) * 10) / 10.0;
        return new Resumen(promedio, aprobadas.size(), aprobadas.stream().map(r -> new ResenaPublica(
                r.getCliente().getNombre() + " " + r.getCliente().getApellido().charAt(0) + ".", r.getCalificacion(),
                r.getComentario(), r.getFecha())).toList());
    }

    @Transactional(readOnly = true)
    public Pagina<ResenaAdmin> listar(EstadoResena estado, Pageable pagina) {
        var resultado = estado == null ? resenas.findAll(pagina) : resenas.findByEstado(estado, pagina);
        return Pagina.de(resultado, ResenaService::admin);
    }

    @Transactional
    public ResenaAdmin moderar(long id, EstadoResena estado) {
        if (estado == null || estado == EstadoResena.PENDIENTE) {
            throw new ConflictoException("Una reseña se aprueba o se rechaza");
        }
        var resena = resenas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("La reseña no existe"));
        resena.moderar(estado);
        return admin(resena);
    }

    private static ResenaAdmin admin(Resena r) {
        return new ResenaAdmin(r.getId(), r.getProducto().getNombre(), r.getProducto().getSlug(),
                r.getCliente().getNombre() + " " + r.getCliente().getApellido(), r.getCliente().getCorreo(),
                r.getCalificacion(), r.getComentario(), r.getFecha(), r.getEstado());
    }
}
