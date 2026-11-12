package co.nalu.datos.repositorio;

import co.nalu.datos.entidad.EstadoPedido;
import co.nalu.datos.entidad.Pedido;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PedidoRepository extends JpaRepository<Pedido, Long>, JpaSpecificationExecutor<Pedido> {

    boolean existsByNumeroPedido(String numeroPedido);

    Optional<Pedido> findByNumeroPedido(String numeroPedido);

    /** Bloquea el pedido: el webhook y las tareas programadas no lo procesan a la vez (plan §6.4). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> bloquear(Long id);

    Optional<Pedido> findByNumeroPedidoAndClienteId(String numeroPedido, Long idCliente);

    Page<Pedido> findByClienteIdOrderByFechaDesc(Long idCliente, Pageable pagina);

    List<Pedido> findByReservaActivaTrueAndReservaExpiraEnBefore(OffsetDateTime ahora);

    List<Pedido> findByEstadoAndFechaBefore(EstadoPedido estado, OffsetDateTime limite);

    /** RF07: el cliente recibió un pedido con alguna variante del producto. */
    @Query("""
            select count(d) > 0 from DetallePedido d
            where d.pedido.cliente.id = :idCliente and d.pedido.estado = co.nalu.datos.entidad.EstadoPedido.ENTREGADO
              and d.variante.producto.id = :idProducto""")
    boolean clienteRecibioProducto(Long idCliente, Long idProducto);
}
