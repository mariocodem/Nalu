package co.nalu.datos.repositorio;

import co.nalu.datos.entidad.IntentoLogin;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface IntentoLoginRepository extends JpaRepository<IntentoLogin, Long> {

    long countByCorreoAndExitosoFalseAndFechaAfter(String correo, OffsetDateTime desde);

    Optional<IntentoLogin> findFirstByCorreoAndExitosoTrueOrderByFechaDesc(String correo);

    @Modifying
    @Query("delete from IntentoLogin i where i.fecha < :limite")
    int eliminarAnterioresA(OffsetDateTime limite);
}
