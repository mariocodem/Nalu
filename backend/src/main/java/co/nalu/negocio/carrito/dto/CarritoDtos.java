package co.nalu.negocio.carrito.dto;

import am.ik.yavi.core.Validator;
import co.nalu.negocio.comun.EsquemaValidacion;
import java.math.BigDecimal;
import java.util.List;

/** RF08. Contrato: docs/arquitectura/api-fase3.md §1. */
public final class CarritoDtos {

    private CarritoDtos() {}

    /**
     * @param maximo unidades que se pueden pedir de esta línea (existencias libres, tope por ítem)
     * @param disponible {@code false} si la pieza se retiró del catálogo o se agotó
     */
    public record ItemCarrito(Long id, Long idVariante, String slug, String nombre, String talla, String imagen,
                              String textoAlternativo, BigDecimal precioUnitario, int cantidad, BigDecimal subtotal,
                              int maximo, boolean disponible) {}

    /** {@code token}: identificador del carrito de visitante; {@code null} si es el de un cliente. */
    public record Carrito(String token, List<ItemCarrito> items, int cantidadTotal, BigDecimal subtotal) {}

    public record AgregarItemRequest(Long idVariante, Integer cantidad) {
        public static final Validator<AgregarItemRequest> ESQUEMA = EsquemaValidacion.para(AgregarItemRequest.class)
                ._long(AgregarItemRequest::idVariante, "idVariante", c -> c.notNull())
                ._integer(AgregarItemRequest::cantidad, "cantidad", c -> c.notNull().greaterThan(0)
                        .lessThanOrEqual(co.nalu.datos.entidad.Carrito.MAXIMO_POR_ITEM))
                .build();
    }

    public record CambiarCantidadRequest(Integer cantidad) {
        public static final Validator<CambiarCantidadRequest> ESQUEMA = EsquemaValidacion.para(CambiarCantidadRequest.class)
                ._integer(CambiarCantidadRequest::cantidad, "cantidad", c -> c.notNull().greaterThan(0)
                        .lessThanOrEqual(co.nalu.datos.entidad.Carrito.MAXIMO_POR_ITEM))
                .build();
    }
}
