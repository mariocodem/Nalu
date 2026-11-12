package co.nalu.datos.repositorio;

import co.nalu.datos.entidad.MovimientoInventario;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    List<MovimientoInventario> findByVarianteIdOrderByFechaAsc(Long idVariante);

    Page<MovimientoInventario> findByVarianteId(Long idVariante, Pageable pagina);
}
