package co.nalu.negocio.pedidos;

import co.nalu.config.AppProperties;
import co.nalu.config.VentasProperties;
import co.nalu.datos.entidad.Carrito;
import co.nalu.datos.entidad.Cliente;
import co.nalu.datos.entidad.DetallePedido;
import co.nalu.datos.entidad.ItemCarrito;
import co.nalu.datos.entidad.Pedido;
import co.nalu.datos.entidad.VarianteProducto;
import co.nalu.datos.repositorio.DireccionRepository;
import co.nalu.datos.repositorio.PedidoRepository;
import co.nalu.datos.repositorio.UsuarioRepository;
import co.nalu.negocio.carrito.CarritoService;
import co.nalu.negocio.catalogo.CalculadoraPrecio;
import co.nalu.negocio.comun.ConflictoException;
import co.nalu.negocio.comun.ExistenciasInsuficientesException;
import co.nalu.negocio.comun.RecursoNoEncontradoException;
import co.nalu.negocio.inventario.InventarioService;
import co.nalu.negocio.pagos.PasarelaPagos;
import co.nalu.negocio.pedidos.dto.PedidoDtos.Cotizacion;
import co.nalu.negocio.pedidos.dto.PedidoDtos.CrearPedidoRequest;
import co.nalu.negocio.pedidos.dto.PedidoDtos.Cupon;
import co.nalu.negocio.pedidos.dto.PedidoDtos.LineaCotizada;
import co.nalu.negocio.pedidos.dto.PedidoDtos.PedidoCreado;
import co.nalu.negocio.promociones.PromocionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checkout (plan §6.3, RF09–RF11, RNF10, RNF11). {@link #crearPedido} es una sola transacción:
 * bloquea las variantes en orden de id, verifica existencias, recalcula precios, aplica el cupón,
 * reserva, crea el pedido con su pago iniciado y marca el carrito como convertido. Si algo falla,
 * no queda nada a medias.
 */
@Service
public class CheckoutService {

    static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final CarritoService carritos;
    private final DireccionRepository direcciones;
    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;
    private final InventarioService inventario;
    private final PromocionService promociones;
    private final CalculadoraPrecio calculadora;
    private final GeneradorNumeroPedido numeros;
    private final PasarelaPagos pasarela;
    private final VentasProperties ventas;
    private final AppProperties app;
    private final Clock reloj;

    public CheckoutService(CarritoService carritos, DireccionRepository direcciones, PedidoRepository pedidos,
                           UsuarioRepository usuarios, InventarioService inventario, PromocionService promociones,
                           CalculadoraPrecio calculadora, GeneradorNumeroPedido numeros, PasarelaPagos pasarela,
                           VentasProperties ventas, AppProperties app, Clock reloj) {
        this.carritos = carritos;
        this.direcciones = direcciones;
        this.pedidos = pedidos;
        this.usuarios = usuarios;
        this.inventario = inventario;
        this.promociones = promociones;
        this.calculadora = calculadora;
        this.numeros = numeros;
        this.pasarela = pasarela;
        this.ventas = ventas;
        this.app = app;
        this.reloj = reloj;
    }

    /** Cotización sin crear nada ni reservar: precios, cupón, envío y lo que no alcanza. */
    @Transactional(readOnly = true)
    public Cotizacion cotizar(long idCliente, String codigoCupon) {
        var carrito = carritoConItems(idCliente);
        var lineas = new ArrayList<LineaCotizada>();
        var faltantes = new ArrayList<String>();
        for (var item : carrito.getItems()) {
            var v = item.getVariante();
            boolean alcanza = CarritoService.sePuedeComprar(v) && v.disponible() >= item.getCantidad();
            if (!alcanza) {
                faltantes.add(faltante(v, item.getCantidad()));
            }
            var precio = calculadora.precioVenta(v.getProducto(), v);
            lineas.add(new LineaCotizada(v.getId(), v.getProducto().getNombre(), v.getTalla(), item.getCantidad(), precio,
                    precio.multiply(BigDecimal.valueOf(item.getCantidad())), alcanza));
        }
        var subtotal = lineas.stream().map(LineaCotizada::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        var cupon = promociones.aplicar(codigoCupon, subtotal, hoy());
        var envio = costoEnvio(subtotal.subtract(cupon.descuento()));
        return new Cotizacion(lineas, subtotal, cupon.descuento(), envio, subtotal.subtract(cupon.descuento()).add(envio),
                cuponDto(codigoCupon, cupon), faltantes);
    }

    @Transactional
    public PedidoCreado crearPedido(long idCliente, CrearPedidoRequest solicitud) {
        var cliente = usuarios.findById(idCliente).filter(Cliente.class::isInstance).map(Cliente.class::cast)
                .orElseThrow(() -> new RecursoNoEncontradoException("La cuenta de cliente no existe"));
        var direccion = direcciones.findByIdAndClienteIdAndActivaTrue(solicitud.idDireccion(), idCliente)
                .orElseThrow(() -> new RecursoNoEncontradoException("La dirección no existe"));
        var carrito = carritoConItems(idCliente);

        // 1-2. Bloqueo de filas en orden de id y verificación de existencias.
        Map<Long, VarianteProducto> bloqueadas = inventario.bloquear(
                carrito.getItems().stream().map(i -> i.getVariante().getId()).toList());
        var faltantes = new ArrayList<String>();
        for (var item : carrito.getItems()) {
            var v = bloqueadas.get(item.getVariante().getId());
            if (!CarritoService.sePuedeComprar(v) || v.disponible() < item.getCantidad()) {
                faltantes.add(faltante(v, item.getCantidad()));
            }
        }
        if (!faltantes.isEmpty()) {
            throw new ExistenciasInsuficientesException(faltantes);
        }

        // 3-4. Precios del servidor, cupón y totales.
        var precios = new ArrayList<BigDecimal>();
        var subtotal = BigDecimal.ZERO;
        for (var item : carrito.getItems()) {
            var v = bloqueadas.get(item.getVariante().getId());
            var precio = calculadora.precioVenta(v.getProducto(), v);
            precios.add(precio);
            subtotal = subtotal.add(precio.multiply(BigDecimal.valueOf(item.getCantidad())));
        }
        var cupon = promociones.aplicar(solicitud.codigoCupon(), subtotal, hoy());
        if (cupon.mensaje() != null) {
            throw new ConflictoException(cupon.mensaje());
        }
        var descuentos = prorratear(cupon.descuento(), carrito.getItems(), precios, subtotal);

        // 5-6. Pedido, reserva y pago iniciado.
        var ahora = OffsetDateTime.now(reloj);
        var pedido = new Pedido(numeros.nuevo(), cliente, direccion, cupon.promocion(), ahora.plus(ventas.reservaTtl()));
        for (int i = 0; i < carrito.getItems().size(); i++) {
            var item = carrito.getItems().get(i);
            pedido.agregarDetalle(new DetallePedido(bloqueadas.get(item.getVariante().getId()), item.getCantidad(),
                    precios.get(i), descuentos.get(i)));
        }
        pedido.fijarTotales(subtotal, cupon.descuento(), costoEnvio(subtotal.subtract(cupon.descuento())));
        pedido.registrarCreacion();
        inventario.reservar(pedido);
        var pago = pedido.iniciarPago(pedido.getNumeroPedido() + "-1");
        pedidos.save(pedido);

        // 7. El carrito queda convertido.
        carrito.convertir();

        var datosPago = pasarela.prepararPago(pago.getReferencia(), centavos(pedido.getTotal()), cliente.getCorreo(),
                urlRetorno(pedido.getNumeroPedido()));
        return new PedidoCreado(pedido.getNumeroPedido(), pedido.getEstado(), pedido.getTotal(),
                pedido.getReservaExpiraEn(), datosPago);
    }

    BigDecimal costoEnvio(BigDecimal subtotalConDescuento) {
        return subtotalConDescuento.compareTo(ventas.envioGratisDesde()) >= 0 ? BigDecimal.ZERO.setScale(2)
                : ventas.tarifaEnvio().setScale(2, RoundingMode.HALF_UP);
    }

    String urlRetorno(String numero) {
        return app.origenFrontend() + "/pago/resultado?pedido=" + numero;
    }

    static long centavos(BigDecimal valor) {
        return valor.movePointRight(2).longValueExact();
    }

    LocalDate hoy() {
        return LocalDate.now(reloj.withZone(ZONA));
    }

    /**
     * Reparte el descuento entre las líneas en proporción a su subtotal, en pesos enteros; el
     * redondeo sobrante va a la última línea para que la suma sea exacta.
     */
    static List<BigDecimal> prorratear(BigDecimal descuento, List<ItemCarrito> items, List<BigDecimal> precios,
                                       BigDecimal subtotal) {
        var partes = new ArrayList<BigDecimal>();
        var repartido = BigDecimal.ZERO;
        for (int i = 0; i < items.size(); i++) {
            BigDecimal parte;
            if (descuento.signum() == 0 || subtotal.signum() == 0) {
                parte = BigDecimal.ZERO;
            } else if (i == items.size() - 1) {
                parte = descuento.subtract(repartido);
            } else {
                var lineal = precios.get(i).multiply(BigDecimal.valueOf(items.get(i).getCantidad()));
                parte = descuento.multiply(lineal).divide(subtotal, 0, RoundingMode.DOWN);
            }
            repartido = repartido.add(parte);
            partes.add(parte.setScale(2, RoundingMode.UNNECESSARY));
        }
        return partes;
    }

    private Carrito carritoConItems(long idCliente) {
        var carrito = carritos.activoDelCliente(idCliente)
                .orElseThrow(() -> new ConflictoException("Tu bolsa está vacía"));
        if (carrito.getItems().isEmpty()) {
            throw new ConflictoException("Tu bolsa está vacía");
        }
        return carrito;
    }

    private static Cupon cuponDto(String codigo, PromocionService.CuponAplicado cupon) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        return new Cupon(codigo.strip().toUpperCase(java.util.Locale.ROOT), cupon.promocion() != null, cupon.mensaje());
    }

    private static String faltante(VarianteProducto v, int pedidas) {
        var nombre = v.getProducto().getNombre() + (v.getTalla() == null ? "" : " (talla " + v.getTalla() + ")");
        if (!CarritoService.sePuedeComprar(v) || v.disponible() <= 0) {
            return nombre + ": agotada";
        }
        return nombre + ": pediste " + pedidas + " y quedan " + v.disponible();
    }
}
