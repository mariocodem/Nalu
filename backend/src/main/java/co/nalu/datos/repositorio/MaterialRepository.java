package co.nalu.datos.repositorio;

import co.nalu.datos.entidad.Material;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    List<Material> findAllByOrderByTipoAscLeyAsc();

    boolean existsByTipoIgnoreCaseAndLey(String tipo, String ley);

    boolean existsByTipoIgnoreCaseAndLeyIsNull(String tipo);
}
