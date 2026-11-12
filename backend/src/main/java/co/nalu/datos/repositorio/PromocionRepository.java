package co.nalu.datos.repositorio;

import co.nalu.datos.entidad.Promocion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromocionRepository extends JpaRepository<Promocion, Long> {

    Optional<Promocion> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<Promocion> findAllByOrderByFechaFinDesc();
}
