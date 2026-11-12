package co.nalu.datos.repositorio;

import co.nalu.datos.entidad.Pago;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByReferencia(String referencia);
}
