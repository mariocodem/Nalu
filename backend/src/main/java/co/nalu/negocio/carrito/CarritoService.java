package co.nalu.negocio.carrito;

import co.nalu.datos.entidad.Carrito;
import co.nalu.datos.entidad.Cliente;
import co.nalu.datos.entidad.EstadoCarrito;
import co.nalu.datos.entidad.EstadoProducto;
import co.nalu.datos.entidad.ImagenProducto;
import co.nalu.datos.entidad.ItemCarrito;
import co.nalu.datos.entidad.VarianteProducto;
import co.nalu.datos.repositorio.CarritoRepository;
import co.nalu.datos.repositorio.UsuarioRepository;
import co.nalu.datos.repositorio.VarianteProductoRepository;
import co.nalu.negocio.carrito.dto.CarritoDtos;
import co.nalu.negocio.carrito.dto.CarritoDtos.AgregarItemRequest;
import co.nalu.negocio.catalogo.CalculadoraPrecio;
import co.nalu.negocio.comun.ConflictoException;
import co.nalu.negocio.comun.RecursoNoEncontradoException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF08: carrito de visitante (por token) o de cliente. Los precios se recalculan siempre con el
 * precio vigente; nunca se aceptan precios del cliente.
 */
@Service
public class CarritoService {

    private final CarritoRepository carritos;
    private final VarianteProductoRepository variantes;
    private final UsuarioRepository usuarios;
    private final CalculadoraPrecio calculadora;
    private final SecureRandom aleatorio = new SecureRandom();

    public CarritoService(CarritoRepository carritos, VarianteProductoRepository variantes, UsuarioRepository usuarios,
                          CalculadoraPrecio calculadora) {
        this.carritos = carritos;
        this.variantes = variantes;
        this.usuarios = usuarios;
        this.calculadora = calculadora;
    }

    @Transactional
    public CarritoDtos.Carrito ver(Long idCliente, String token) {
        return buscar(idCliente, token).map(this::dto).orElseGet(() -> new CarritoDtos.Carrito(null, List.of(), 0, BigDecimal.ZERO));
    }

    @Transactional
    public CarritoDtos.Carrito agregar(Long idCliente, String token, AgregarItemRequest solicitud) {
        var variante = variantes.findById(solicitud.idVariante())
                .filter(CarritoService::sePuedeComprar)
                .orElseThrow(() -> new RecursoNoEncontradoException("La pieza no existe o ya no está a la venta"));
        var carrito = buscar(idCliente, token).orElseGet(() -> carritos.save(new Carrito(nuevoToken(), cliente(idCliente))));
        int enCarrito = carrito.item(variante).map(ItemCarrito::getCantidad).orElse(0);
        verificarExistencias(variante, enCarrito + solicitud.cantidad());
        carrito.agregar(variante, solicitud.cantidad(), precio(variante));
        carritos.flush(); // el ítem nuevo necesita su id para la respuesta
        return dto(carrito);
    }

    @Transactional
    public CarritoDtos.Carrito cambiarCantidad(Long idCliente, String token, long idItem, int cantidad) {
        var carrito = buscar(idCliente, token).orElseThrow(CarritoService::itemInexistente);
        var item = item(carrito, idItem);
        verificarExistencias(item.getVariante(), cantidad);
        item.cambiarCantidad(cantidad, precio(item.getVariante()));
        return dto(carrito);
    }

    @Transactional
    public CarritoDtos.Carrito quitar(Long idCliente, String token, long idItem) {
        var carrito = buscar(idCliente, token).orElseThrow(CarritoService::itemInexistente);
        carrito.quitar(item(carrito, idItem));
        return dto(carrito);
    }

    /**
     * Al iniciar sesión: el carrito del visitante pasa al cliente. Si el cliente ya tenía uno, se
     * suman los ítems (con el tope por ítem) y el del visitante queda FUSIONADO.
     */
    @Transactional
    public CarritoDtos.Carrito fusionar(long idCliente, String token) {
        var visitante = token == null ? Optional.<Carrito>empty()
                : carritos.findByTokenAndEstado(token, EstadoCarrito.ACTIVO).filter(c -> c.getCliente() == null);
        var propio = carritos.findFirstByClienteIdAndEstadoOrderByIdDesc(idCliente, EstadoCarrito.ACTIVO);
        if (visitante.isPresent() && propio.isPresent()) {
            propio.get().absorber(visitante.get());
            carritos.flush();
        } else if (visitante.isPresent()) {
            visitante.get().asignarCliente(cliente(idCliente));
            return dto(visitante.get());
        }
        return propio.map(this::dto).orElseGet(() -> new CarritoDtos.Carrito(null, List.of(), 0, BigDecimal.ZERO));
    }

    /** Carrito activo del cliente, para el checkout. */
    @Transactional(readOnly = true)
    public Optional<Carrito> activoDelCliente(long idCliente) {
        return carritos.findFirstByClienteIdAndEstadoOrderByIdDesc(idCliente, EstadoCarrito.ACTIVO);
    }

    public static boolean sePuedeComprar(VarianteProducto v) {
        return v.isActiva() && v.getProducto().getEstado() == EstadoProducto.PUBLICADO
                && v.getProducto().getCategoria().isActiva();
    }

    private Optional<Carrito> buscar(Long idCliente, String token) {
        if (idCliente != null) {
            return carritos.findFirstByClienteIdAndEstadoOrderByIdDesc(idCliente, EstadoCarrito.ACTIVO);
        }
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return carritos.findByTokenAndEstado(token, EstadoCarrito.ACTIVO).filter(c -> c.getCliente() == null);
    }

    private CarritoDtos.Carrito dto(Carrito carrito) {
        var items = carrito.getItems().stream().map(i -> {
            var v = i.getVariante();
            boolean disponible = sePuedeComprar(v) && v.disponible() > 0;
            var precio = precio(v);
            i.actualizarPrecio(precio);
            var imagen = v.getProducto().getImagenes().stream().filter(ImagenProducto::isPrincipal).findFirst();
            return new CarritoDtos.ItemCarrito(i.getId(), v.getId(), v.getProducto().getSlug(), v.getProducto().getNombre(),
                    v.getTalla(), imagen.map(ImagenProducto::getUrl).orElse(null),
                    imagen.map(ImagenProducto::getTextoAlternativo).orElse(null), precio, i.getCantidad(),
                    precio.multiply(BigDecimal.valueOf(i.getCantidad())),
                    Math.min(Math.max(v.disponible(), 0), Carrito.MAXIMO_POR_ITEM), disponible);
        }).toList();
        var subtotal = items.stream().filter(CarritoDtos.ItemCarrito::disponible)
                .map(CarritoDtos.ItemCarrito::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        int cantidad = items.stream().mapToInt(CarritoDtos.ItemCarrito::cantidad).sum();
        return new CarritoDtos.Carrito(carrito.getCliente() == null ? carrito.getToken() : null, items, cantidad, subtotal);
    }

    private BigDecimal precio(VarianteProducto v) {
        return calculadora.precioVenta(v.getProducto(), v);
    }

    private static void verificarExistencias(VarianteProducto variante, int cantidad) {
        if (cantidad > variante.disponible()) {
            throw new ConflictoException(variante.disponible() <= 0 ? "Esta pieza está agotada"
                    : "Solo quedan " + variante.disponible() + " unidades de esta pieza");
        }
    }

    private Cliente cliente(Long idCliente) {
        if (idCliente == null) {
            return null;
        }
        return usuarios.findById(idCliente).filter(Cliente.class::isInstance).map(Cliente.class::cast)
                .orElseThrow(() -> new RecursoNoEncontradoException("La cuenta de cliente no existe"));
    }

    private static ItemCarrito item(Carrito carrito, long idItem) {
        return carrito.getItems().stream().filter(i -> i.getId().equals(idItem)).findFirst()
                .orElseThrow(CarritoService::itemInexistente);
    }

    private static RecursoNoEncontradoException itemInexistente() {
        return new RecursoNoEncontradoException("Ese artículo no está en tu bolsa");
    }

    private String nuevoToken() {
        var bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
