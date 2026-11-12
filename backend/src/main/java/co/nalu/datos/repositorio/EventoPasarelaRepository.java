package co.nalu.datos.repositorio;

import co.nalu.datos.entidad.EventoPasarela;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoPasarelaRepository extends JpaRepository<EventoPasarela, Long> {

    boolean existsByClaveIdempotencia(String claveIdempotencia);
}
