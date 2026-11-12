package co.nalu.negocio.pedidos;

import co.nalu.datos.entidad.Cliente_;
import co.nalu.datos.entidad.DetallePedido;
import co.nalu.datos.entidad.Envio;
import co.nalu.datos.entidad.EstadoPedido;
import co.nalu.datos.entidad.Pago;
import co.nalu.datos.entidad.Pedido;
import co.nalu.datos.entidad.Pedido_;
import co.nalu.datos.entidad.Usuario;
import co.nalu.datos.repositorio.PedidoRepository;
import co.nalu.datos.repositorio.UsuarioRepository;
import co.nalu.negocio.comun.ConflictoException;
import co.nalu.negocio.comun.Pagina;
import co.nalu.negocio.comun.RecursoNoEncontradoException;
import co.nalu.negocio.inventario.InventarioService;
import co.nalu.negocio.notificaciones.EventosPedido;
import co.nalu.negocio.pedidos.dto.GestionPedidoDtos.CambioEstadoRequest;
import co.nalu.negocio.pedidos.dto.GestionPedidoDtos.ClienteAdmin;
import co.nalu.negocio.pedidos.dto.GestionPedidoDtos.EnvioRequest;
import co.nalu.negocio.pedidos.dto.GestionPedidoDtos.FiltroPedidos;
import co.nalu.negocio.pedidos.dto.GestionPedidoDtos.PagoAdmin;
import co.nalu.negocio.pedidos.dto.GestionPedidoDtos.PedidoAdminDetalle;
import co.nalu.negocio.pedidos.dto.GestionPedidoDtos.PedidoAdminResumen;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Locale;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF14 (administración): ciclo de vida del pedido según la máquina de estados del plan §6.6.
 * PAGADO solo lo pone el webhook; ENVIADO solo se alcanza registrando el envío.
 */
@Service
public class GestionPedidoService {

    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;
    private final InventarioService inventario;
    private final ApplicationEventPublisher publicador;
    private final Clock reloj;

    public GestionPedidoService(PedidoRepository pedidos, UsuarioRepository usuarios, InventarioService inventario,
                                ApplicationEventPublisher publicador, Clock reloj) {
        this.pedidos = pedidos;
        this.usuarios = usuarios;
        this.inventario = inventario;
        this.publicador = publicador;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Pagina<PedidoAdminResumen> listar(FiltroPedidos filtro, Pageable pagina) {
        Specification<Pedido> spec = (r, q, cb) -> {
            var condiciones = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (filtro.estado() != null) {
                condiciones.add(cb.equal(r.get(Pedido_.estado), filtro.estado()));
            }
            if (filtro.desde() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(r.get(Pedido_.fecha),
                        filtro.desde().atStartOfDay(CheckoutService.ZONA).toOffsetDateTime()));
            }
            if (filtro.hasta() != null) {
                condiciones.add(cb.lessThan(r.get(Pedido_.fecha),
                        filtro.hasta().plusDays(1).atStartOfDay(CheckoutService.ZONA).toOffsetDateTime()));
            }
            if (filtro.q() != null && !filtro.q().isBlank()) {
                var patron = "%" + filtro.q().strip().toLowerCase(Locale.ROOT).replace("%", "\\%").replace("_", "\\_") + "%";
                var cliente = r.join(Pedido_.cliente);
                condiciones.add(cb.or(cb.like(cb.lower(r.get(Pedido_.numeroPedido)), patron, '\\'),
                        cb.like(cb.lower(cliente.get(Cliente_.correo)), patron, '\\')));
            }
            return cb.and(condiciones.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return Pagina.de(pedidos.findAll(spec, pagina), p -> new PedidoAdminResumen(p.getNumeroPedido(), p.getFecha(),
                p.getCliente().getNombre() + " " + p.getCliente().getApellido(), p.getCliente().getCorreo(), p.getEstado(),
                p.getTotal(), p.getDetalles().stream().mapToInt(DetallePedido::getCantidad).sum()));
    }

    @Transactional(readOnly = true)
    public PedidoAdminDetalle detalle(String numero) {
        return detalle(buscar(numero));
    }

    @Transactional
    public PedidoAdminDetalle cambiarEstado(String numero, CambioEstadoRequest solicitud, long idAdministrador) {
        var pedido = pedidos.bloquear(buscar(numero).getId()).orElseThrow();
        var destino = solicitud.estado();
        var admin = administrador(idAdministrador);
        if (destino == EstadoPedido.PAGADO) {
            throw new ConflictoException("Un pedido solo pasa a pagado cuando la pasarela confirma el pago");
        }
        if (destino == EstadoPedido.ENVIADO) {
            throw new ConflictoException("Para marcarlo como enviado, registra la transportadora y la guía");
        }
        if (!pedido.getEstado().puedeTransicionarA(destino)) {
            throw new ConflictoException("No se puede pasar un pedido de " + nombre(pedido.getEstado()) + " a " + nombre(destino));
        }
        switch (destino) {
            case CANCELADO -> cancelar(pedido, admin, solicitud.nota());
            case ENTREGADO -> {
                pedido.getEnvio().registrarEntrega(OffsetDateTime.now(reloj));
                pedido.cambiarEstado(EstadoPedido.ENTREGADO, admin, solicitud.nota());
            }
            default -> pedido.cambiarEstado(destino, admin, solicitud.nota());
        }
        return detalle(pedido);
    }

    @Transactional
    public PedidoAdminDetalle registrarEnvio(String numero, EnvioRequest solicitud, long idAdministrador) {
        var pedido = pedidos.bloquear(buscar(numero).getId()).orElseThrow();
        if (pedido.getEstado() != EstadoPedido.EN_PREPARACION) {
            throw new ConflictoException("Solo se despachan pedidos en preparación");
        }
        var costo = solicitud.costo() == null ? pedido.getCostoEnvio() : solicitud.costo();
        pedido.registrarEnvio(new Envio(pedido, solicitud.transportadora(), solicitud.numeroGuia(), costo,
                OffsetDateTime.now(reloj)));
        pedido.cambiarEstado(EstadoPedido.ENVIADO, administrador(idAdministrador),
                solicitud.transportadora() + " · guía " + solicitud.numeroGuia());
        publicador.publishEvent(new EventosPedido.PedidoEnviado(pedido.getNumeroPedido()));
        return detalle(pedido);
    }

    /**
     * Sin pagar: se libera la reserva. Ya pagado: el motivo es obligatorio y las unidades vuelven
     * al inventario; el reembolso se gestiona fuera de Nalu.
     */
    private void cancelar(Pedido pedido, Usuario admin, String nota) {
        if (pedido.getEstado() == EstadoPedido.PENDIENTE_PAGO) {
            if (pedido.isReservaActiva()) {
                inventario.liberar(pedido);
            }
        } else {
            if (nota == null) {
                throw new ConflictoException("Indica el motivo de la cancelación de un pedido pagado");
            }
            inventario.reintegrar(pedido);
        }
        pedido.cambiarEstado(EstadoPedido.CANCELADO, admin, nota == null ? "Cancelado por la tienda" : nota);
        publicador.publishEvent(new EventosPedido.PedidoCancelado(pedido.getNumeroPedido()));
    }

    private PedidoAdminDetalle detalle(Pedido p) {
        var base = PedidoClienteService.detalle(p);
        var c = p.getCliente();
        var pagos = p.getPagos().stream().sorted(Comparator.comparing(Pago::getId))
                .map(pg -> new PagoAdmin(pg.getReferencia(), pg.getEstado(), pg.getMetodo(), pg.getMonto(),
                        pg.getReferenciaExterna(), pg.getFechaTransaccion() == null ? pg.getFechaCreacion() : pg.getFechaTransaccion()))
                .toList();
        var siguientes = p.getEstado().siguientes().stream().filter(e -> e != EstadoPedido.PAGADO).sorted().toList();
        return new PedidoAdminDetalle(base, new ClienteAdmin(c.getNombre() + " " + c.getApellido(), c.getCorreo(),
                c.getTelefono()), pagos, siguientes);
    }

    private Pedido buscar(String numero) {
        return pedidos.findByNumeroPedido(numero).orElseThrow(() -> new RecursoNoEncontradoException("El pedido no existe"));
    }

    private Usuario administrador(long id) {
        return usuarios.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("El administrador no existe"));
    }

    private static String nombre(EstadoPedido e) {
        return e.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
