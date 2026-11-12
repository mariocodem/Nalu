package co.nalu.datos.entidad;

public enum EstadoCarrito {
    /** Carrito en uso; un cliente tiene como máximo uno (plan §5.5). */
    ACTIVO,
    /** Se convirtió en pedido. */
    CONVERTIDO,
    /** Sus ítems se pasaron al carrito del cliente al iniciar sesión. */
    FUSIONADO
}
