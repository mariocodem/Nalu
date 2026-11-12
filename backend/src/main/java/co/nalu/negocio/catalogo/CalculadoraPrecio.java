package co.nalu.negocio.catalogo;

import co.nalu.datos.entidad.Producto;
import co.nalu.datos.entidad.ProductoMaterial;
import co.nalu.datos.entidad.RolMaterial;
import co.nalu.datos.entidad.VarianteProducto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Precio de venta (ADR 003, plan §6.1):
 * {@code precio_base × factor_precio(METAL_PRINCIPAL) + precio_adicional}, redondeado a pesos
 * enteros con HALF_UP. Sin metal principal el factor es 1. Es el único lugar donde se calcula un
 * precio: el frontend nunca lo envía.
 */
@Component
public class CalculadoraPrecio {

    public BigDecimal precioVenta(Producto producto, VarianteProducto variante) {
        return producto.getPrecioBase().multiply(factor(producto)).add(variante.getPrecioAdicional())
                .setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.UNNECESSARY);
    }

    /** El más bajo entre las variantes activas, o {@code null} si no hay ninguna. */
    public BigDecimal precioDesde(Producto producto) {
        return producto.getVariantes().stream()
                .filter(VarianteProducto::isActiva)
                .map(v -> precioVenta(producto, v))
                .min(BigDecimal::compareTo)
                .orElse(null);
    }

    private static BigDecimal factor(Producto producto) {
        return producto.getMateriales().stream()
                .filter(pm -> pm.getRol() == RolMaterial.METAL_PRINCIPAL)
                .map(ProductoMaterial::getMaterial)
                .map(m -> m.getFactorPrecio())
                .findFirst()
                .orElse(BigDecimal.ONE);
    }
}
