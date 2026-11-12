package co.nalu.datos.repositorio;

import co.nalu.datos.entidad.Direccion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DireccionRepository extends JpaRepository<Direccion, Long> {

    List<Direccion> findByClienteIdAndActivaTrueOrderByIdAsc(Long idCliente);

    /** Filtra por dueño: una dirección ajena se trata como inexistente (RNF02). */
    Optional<Direccion> findByIdAndClienteIdAndActivaTrue(Long id, Long idCliente);
}
