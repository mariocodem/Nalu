package co.nalu.negocio.reportes;

import co.nalu.datos.entidad.EstadoPedido;
import co.nalu.datos.entidad.RolMaterial;
import co.nalu.datos.repositorio.ReporteRepository;
import co.nalu.negocio.comun.ConflictoException;
import co.nalu.negocio.inventario.InventarioAdminService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF15: reportes por rango de fechas (zona America/Bogota, ambos días incluidos). Cuentan como
 * venta los pedidos PAGADO, EN_PREPARACION, ENVIADO y ENTREGADO. Cada reporte es una tabla:
 * columnas y filas, lista para JSON o CSV.
 */
@Service
public class ReporteService {

    public record Tabla(String titulo, List<String> columnas, List<List<Object>> filas, Map<String, Object> resumen) {}

    static final ZoneId ZONA = ZoneId.of("America/Bogota");
    static final EnumSet<EstadoPedido> VENDIDOS =
            EnumSet.of(EstadoPedido.PAGADO, EstadoPedido.EN_PREPARACION, EstadoPedido.ENVIADO, EstadoPedido.ENTREGADO);
    static final List<String> TIPOS = List.of("ventas", "rotacion", "agotados", "cupones", "pagos");

    private final ReporteRepository reportes;
    private final InventarioAdminService inventario;

    public ReporteService(ReporteRepository reportes, InventarioAdminService inventario) {
        this.reportes = reportes;
        this.inventario = inventario;
    }

    @Transactional(readOnly = true)
    public Tabla generar(String tipo, LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new ConflictoException("La fecha inicial debe ser anterior a la final");
        }
        OffsetDateTime inicio = desde.atStartOfDay(ZONA).toOffsetDateTime();
        OffsetDateTime fin = hasta.plusDays(1).atStartOfDay(ZONA).toOffsetDateTime();
        return switch (tipo) {
            case "ventas" -> {
                var r = reportes.resumen(VENDIDOS, inicio, fin);
                var ticket = r.pedidos() == 0 ? BigDecimal.ZERO
                        : r.ingresos().divide(BigDecimal.valueOf(r.pedidos()), 0, RoundingMode.HALF_UP);
                yield new Tabla("Ventas por material (metal principal)", List.of("Material", "Unidades", "Valor"),
                        reportes.ventasPorMaterial(VENDIDOS, inicio, fin, RolMaterial.METAL_PRINCIPAL).stream()
                                .map(v -> List.<Object>of(v.material(), v.unidades(), v.valor())).toList(),
                        Map.of("pedidos", r.pedidos(), "ingresos", r.ingresos(), "descuentos", r.descuentos(),
                                "envios", r.envios(), "ticketPromedio", ticket));
            }
            case "rotacion" -> new Tabla("Piezas más vendidas", List.of("SKU", "Producto", "Unidades", "Valor"),
                    reportes.ventasPorProducto(VENDIDOS, inicio, fin).stream()
                            .map(v -> List.<Object>of(v.sku(), v.producto(), v.unidades(), v.valor())).toList(), Map.of());
            case "agotados" -> new Tabla("Variantes agotadas", List.of("SKU", "Producto", "Talla", "Existencia", "Reservadas"),
                    inventario.alertas(0).stream().map(a -> List.<Object>of(a.sku(), a.producto(),
                            a.talla() == null ? "" : a.talla(), a.stockActual(), a.stockReservado())).toList(), Map.of());
            case "cupones" -> new Tabla("Uso de cupones", List.of("Cupón", "Pedidos", "Descuento"),
                    reportes.usoDeCupones(VENDIDOS, inicio, fin).stream()
                            .map(c -> List.<Object>of(c.codigo(), c.pedidos(), c.descuento())).toList(), Map.of());
            case "pagos" -> new Tabla("Pagos por estado", List.of("Estado", "Intentos", "Monto"),
                    reportes.pagosPorEstado(inicio, fin).stream()
                            .map(p -> List.<Object>of(p.estado(), p.cantidad(), p.monto())).toList(), Map.of());
            default -> throw new co.nalu.negocio.comun.RecursoNoEncontradoException("El reporte no existe");
        };
    }

    /** CSV con separador coma, comillas donde haga falta y BOM para que Excel lea las tildes. */
    public static String csv(Tabla tabla) {
        var sb = new StringBuilder("﻿");
        sb.append(String.join(",", tabla.columnas().stream().map(ReporteService::celda).toList())).append("\r\n");
        for (var fila : tabla.filas()) {
            sb.append(String.join(",", fila.stream().map(ReporteService::celda).toList())).append("\r\n");
        }
        return sb.toString();
    }

    private static String celda(Object valor) {
        var texto = valor instanceof BigDecimal b ? b.stripTrailingZeros().toPlainString() : String.valueOf(valor);
        // Evita la inyección de fórmulas al abrir el CSV en una hoja de cálculo.
        if (!texto.isEmpty() && "=+-@".indexOf(texto.charAt(0)) >= 0 && !(valor instanceof Number)) {
            texto = "'" + texto;
        }
        return texto.contains(",") || texto.contains("\"") || texto.contains("\n")
                ? "\"" + texto.replace("\"", "\"\"") + "\"" : texto;
    }
}
