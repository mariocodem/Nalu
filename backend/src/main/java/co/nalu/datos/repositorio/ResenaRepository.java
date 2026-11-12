package co.nalu.datos.repositorio;

import co.nalu.datos.entidad.EstadoResena;
import co.nalu.datos.entidad.Resena;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResenaRepository extends JpaRepository<Resena, Long> {

    boolean existsByProductoIdAndClienteId(Long idProducto, Long idCliente);

    List<Resena> findByProductoIdAndEstadoOrderByFechaDesc(Long idProducto, EstadoResena estado);

    Page<Resena> findByEstado(EstadoResena estado, Pageable pagina);
}
