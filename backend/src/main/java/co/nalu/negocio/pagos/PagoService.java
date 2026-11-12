package co.nalu.negocio.pagos;

import co.nalu.datos.entidad.EstadoPago;
import co.nalu.datos.entidad.EstadoPedido;
import co.nalu.datos.entidad.EventoPasarela;
import co.nalu.datos.entidad.Pago;
import co.nalu.datos.entidad.Pedido;
import co.nalu.datos.repositorio.EventoPasarelaRepository;
import co.nalu.datos.repositorio.PagoRepository;
import co.nalu.datos.repositorio.PedidoRepository;
import co.nalu.negocio.comun.ConflictoException;
import co.nalu.negocio.comun.ExistenciasInsuficientesException;
import co.nalu.negocio.comun.RecursoNoEncontradoException;
import co.nalu.config.AppProperties;
import co.nalu.config.VentasProperties;
import co.nalu.negocio.inventario.InventarioService;
import co.nalu.negocio.notificaciones.EventosPedido;
import co.nalu.negocio.pagos.PasarelaPagos.DatosPago;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirmación de pagos por webhook (plan §6.4, RF12, RNF03) y reintento de pago. El estado de un
 * pedido solo cambia por un evento firmado de la pasarela, nunca por la redirección del navegador.
 */
@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    private final PasarelaPagos pasarela;
    private final EventoPasarelaRepository eventos;
    private final PagoRepository pagos;
    private final PedidoRepository pedidos;
    private final InventarioService inventario;
    private final ApplicationEventPublisher publicador;
    private final VentasProperties ventas;
    private final AppProperties app;
    private final Clock reloj;

    public PagoService(PasarelaPagos pasarela, EventoPasarelaRepository eventos, PagoRepository pagos,
                       PedidoRepository pedidos, InventarioService inventario, ApplicationEventPublisher publicador,
                       VentasProperties ventas, AppProperties app, Clock reloj) {
        this.pasarela = pasarela;
        this.eventos = eventos;
        this.pagos = pagos;
        this.pedidos = pedidos;
        this.inventario = inventario;
        this.publicador = publicador;
        this.ventas = ventas;
        this.app = app;
        this.reloj = reloj;
    }

    /**
     * Procesa un evento de la pasarela. Firma inválida → {@link FirmaInvalidaException} (401).
     * Un evento repetido (misma transacción y estado) no hace nada. Un evento de una referencia
     * desconocida se registra y se ignora: la pasarela no debe reintentarlo.
     */
    @Transactional
    public void procesarEvento(String cuerpo) {
        var evento = pasarela.verificarEvento(cuerpo);
        var clave = evento.idTransaccion() + ":" + evento.estado();
        if (eventos.existsByClaveIdempotencia(clave)) {
            log.info("Evento de pago repetido, se ignora: {}", clave);
            return;
        }
        eventos.saveAndFlush(new EventoPasarela(clave, evento.tipo(), evento.carga()));

        var pago = pagos.findByReferencia(evento.referencia()).orElse(null);
        if (pago == null) {
            log.warn("Evento de pago con referencia desconocida: {}", evento.referencia());
            return;
        }
        var pedido = pedidos.bloquear(pago.getPedido().getId()).orElseThrow();
        var ahora = OffsetDateTime.now(reloj);
        var estado = traducir(evento.estado());

        if (estado == EstadoPago.APROBADO) {
            if (evento.montoCentavos() != pago.getMonto().movePointRight(2).longValueExact()) {
                log.error("Monto del pago {} no coincide: esperado {} y recibido {} centavos", pago.getReferencia(),
                        pago.getMonto(), evento.montoCentavos());
                pago.registrarResultado(EstadoPago.ERROR, evento.idTransaccion(), evento.metodo(), ahora);
                return;
            }
            pago.registrarResultado(EstadoPago.APROBADO, evento.idTransaccion(), evento.metodo(), ahora);
            aprobar(pedido);
        } else if (estado == EstadoPago.PENDIENTE) {
            pago.registrarResultado(EstadoPago.PENDIENTE, evento.idTransaccion(), evento.metodo(), ahora);
        } else {
            pago.registrarResultado(estado, evento.idTransaccion(), evento.metodo(), ahora);
            if (pedido.getEstado() == EstadoPedido.PENDIENTE_PAGO && pedido.isReservaActiva()) {
                inventario.liberar(pedido);
            }
            publicador.publishEvent(new EventosPedido.PagoRechazado(pedido.getNumeroPedido()));
        }
    }

    /** Solo el primer pago aprobado cambia el pedido; uno tardío de un pedido ya pagado se registra. */
    private void aprobar(Pedido pedido) {
        if (pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO) {
            log.error("Pago aprobado para el pedido {} en estado {}: requiere revisión (posible doble cobro)",
                    pedido.getNumeroPedido(), pedido.getEstado());
            return;
        }
        boolean despachable = inventario.confirmarSalida(pedido);
        pedido.cambiarEstado(EstadoPedido.PAGADO, null, despachable ? "Pago aprobado"
                : "Pago aprobado tras expirar la reserva y SIN existencias suficientes: revisar y reembolsar");
        if (pedido.getPromocion() != null) {
            pedido.getPromocion().registrarUso();
        }
        publicador.publishEvent(new EventosPedido.PedidoPagado(pedido.getNumeroPedido()));
    }

    /**
     * Nuevo intento de pago de un pedido pendiente (plan §6.5): si la reserva expiró, vuelve a
     * validar y reservar las existencias.
     */
    @Transactional
    public DatosPago reintentar(long idCliente, String numero) {
        var encontrado = pedidos.findByNumeroPedidoAndClienteId(numero, idCliente)
                .orElseThrow(() -> new RecursoNoEncontradoException("El pedido no existe"));
        var pedido = pedidos.bloquear(encontrado.getId()).orElseThrow();
        if (pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO) {
            throw new ConflictoException("Este pedido ya no está pendiente de pago");
        }
        var ultimo = pedido.getPagos().stream().max(Comparator.comparing(Pago::getId)).map(Pago::getEstado).orElse(null);
        if (ultimo == EstadoPago.PENDIENTE) {
            throw new ConflictoException("Tu pago anterior todavía se está procesando. Espera la confirmación.");
        }
        var ahora = OffsetDateTime.now(reloj);
        if (!pedido.isReservaActiva()) {
            var bloqueadas = inventario.bloquear(pedido.getDetalles().stream().map(d -> d.getVariante().getId()).toList());
            var faltantes = new ArrayList<String>();
            for (var d : pedido.getDetalles()) {
                if (bloqueadas.get(d.getVariante().getId()).disponible() < d.getCantidad()) {
                    faltantes.add(d.getVariante().getProducto().getNombre() + ": ya no hay existencias suficientes");
                }
            }
            if (!faltantes.isEmpty()) {
                throw new ExistenciasInsuficientesException(faltantes);
            }
            inventario.reservar(pedido);
            pedido.renovarReserva(ahora.plus(ventas.reservaTtl()));
        }
        var pago = pedido.iniciarPago(pedido.getNumeroPedido() + "-" + (pedido.getPagos().size() + 1));
        pedidos.flush();
        return pasarela.prepararPago(pago.getReferencia(), pago.getMonto().movePointRight(2).longValueExact(),
                pedido.getCliente().getCorreo(), app.origenFrontend() + "/pago/resultado?pedido=" + numero);
    }

    static EstadoPago traducir(String estadoPasarela) {
        return switch (estadoPasarela) {
            case "APPROVED" -> EstadoPago.APROBADO;
            case "DECLINED" -> EstadoPago.RECHAZADO;
            case "VOIDED" -> EstadoPago.ANULADO;
            case "PENDING" -> EstadoPago.PENDIENTE;
            default -> EstadoPago.ERROR;
        };
    }
}
