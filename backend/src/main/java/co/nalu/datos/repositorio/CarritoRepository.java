package co.nalu.datos.repositorio;

import co.nalu.datos.entidad.Carrito;
import co.nalu.datos.entidad.EstadoCarrito;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    Optional<Carrito> findFirstByClienteIdAndEstadoOrderByIdDesc(Long idCliente, EstadoCarrito estado);

    Optional<Carrito> findByTokenAndEstado(String token, EstadoCarrito estado);
}
