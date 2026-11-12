package co.nalu.datos.repositorio;

import co.nalu.datos.entidad.VarianteProducto;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

public interface VarianteProductoRepository extends JpaRepository<VarianteProducto, Long> {

    /**
     * {@code SELECT … FOR UPDATE} en orden fijo de id: dos compras simultáneas siempre bloquean en
     * el mismo orden y no hay interbloqueos (plan §6.3, RNF11).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    List<VarianteProducto> findByIdInOrderByIdAsc(Collection<Long> ids);

    boolean existsBySku(String sku);

    /** RF13: variantes activas de productos no archivados con pocas existencias libres. */
    @Query("""
            select v from VarianteProducto v join fetch v.producto p
            where v.activa = true and p.estado <> co.nalu.datos.entidad.EstadoProducto.ARCHIVADO
              and (v.stockActual - v.stockReservado) <= :umbral
            order by (v.stockActual - v.stockReservado), v.sku""")
    List<VarianteProducto> conPocasExistencias(int umbral);
}
