package co.nalu.datos.repositorio.proyeccion;

import java.math.BigDecimal;

/** Filas de los reportes (RF15), construidas con JPQL (plan §5.4). */
public final class Proyecciones {

    private Proyecciones() {}

    public record VentaPorMaterial(String material, Long unidades, BigDecimal valor) {}

    public record VentaPorProducto(String sku, String producto, Long unidades, BigDecimal valor) {}

    public record UsoDeCupon(String codigo, Long pedidos, BigDecimal descuento) {}

    public record PagosPorEstado(String estado, Long cantidad, BigDecimal monto) {}

    public record ResumenVentas(Long pedidos, BigDecimal ingresos, BigDecimal descuentos, BigDecimal envios) {}
}
