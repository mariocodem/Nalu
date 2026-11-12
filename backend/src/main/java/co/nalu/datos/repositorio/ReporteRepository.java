package co.nalu.datos.repositorio;

import co.nalu.datos.entidad.DetallePedido;
import co.nalu.datos.entidad.EstadoPedido;
import co.nalu.datos.entidad.RolMaterial;
import co.nalu.datos.repositorio.proyeccion.Proyecciones.PagosPorEstado;
import co.nalu.datos.repositorio.proyeccion.Proyecciones.ResumenVentas;
import co.nalu.datos.repositorio.proyeccion.Proyecciones.UsoDeCupon;
import co.nalu.datos.repositorio.proyeccion.Proyecciones.VentaPorMaterial;
import co.nalu.datos.repositorio.proyeccion.Proyecciones.VentaPorProducto;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Consultas de reportes (RF15) con JPQL y proyecciones a records: sin SQL nativo. */
public interface ReporteRepository extends Repository<DetallePedido, Long> {

    @Query("""
            select new co.nalu.datos.repositorio.proyeccion.Proyecciones$VentaPorMaterial(
                   concat(m.tipo, case when m.ley is null then '' else concat(' ', m.ley) end),
                   sum(d.cantidad), sum(d.precioUnitario * d.cantidad - d.descuentoAplicado))
            from DetallePedido d join d.variante v join v.producto p join p.materiales pm join pm.material m
            where d.pedido.estado in :estados and d.pedido.fecha >= :desde and d.pedido.fecha < :hasta and pm.rol = :rol
            group by m.tipo, m.ley
            order by sum(d.precioUnitario * d.cantidad - d.descuentoAplicado) desc""")
    List<VentaPorMaterial> ventasPorMaterial(Collection<EstadoPedido> estados, OffsetDateTime desde,
                                             OffsetDateTime hasta, RolMaterial rol);

    @Query("""
            select new co.nalu.datos.repositorio.proyeccion.Proyecciones$VentaPorProducto(
                   p.sku, p.nombre, sum(d.cantidad), sum(d.precioUnitario * d.cantidad - d.descuentoAplicado))
            from DetallePedido d join d.variante v join v.producto p
            where d.pedido.estado in :estados and d.pedido.fecha >= :desde and d.pedido.fecha < :hasta
            group by p.id, p.sku, p.nombre
            order by sum(d.cantidad) desc""")
    List<VentaPorProducto> ventasPorProducto(Collection<EstadoPedido> estados, OffsetDateTime desde, OffsetDateTime hasta);

    @Query("""
            select new co.nalu.datos.repositorio.proyeccion.Proyecciones$ResumenVentas(
                   count(p), coalesce(sum(p.total), 0), coalesce(sum(p.descuento), 0), coalesce(sum(p.costoEnvio), 0))
            from Pedido p
            where p.estado in :estados and p.fecha >= :desde and p.fecha < :hasta""")
    ResumenVentas resumen(Collection<EstadoPedido> estados, OffsetDateTime desde, OffsetDateTime hasta);

    @Query("""
            select new co.nalu.datos.repositorio.proyeccion.Proyecciones$UsoDeCupon(pr.codigo, count(p), sum(p.descuento))
            from Pedido p join p.promocion pr
            where p.estado in :estados and p.fecha >= :desde and p.fecha < :hasta
            group by pr.codigo
            order by count(p) desc""")
    List<UsoDeCupon> usoDeCupones(Collection<EstadoPedido> estados, OffsetDateTime desde, OffsetDateTime hasta);

    @Query("""
            select new co.nalu.datos.repositorio.proyeccion.Proyecciones$PagosPorEstado(cast(pg.estado as String), count(pg), sum(pg.monto))
            from Pago pg
            where pg.fechaCreacion >= :desde and pg.fechaCreacion < :hasta
            group by pg.estado
            order by count(pg) desc""")
    List<PagosPorEstado> pagosPorEstado(OffsetDateTime desde, OffsetDateTime hasta);
}
