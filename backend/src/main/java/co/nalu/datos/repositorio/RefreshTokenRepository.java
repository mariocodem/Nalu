package co.nalu.datos.repositorio;

import co.nalu.datos.entidad.RefreshToken;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Bloquea la fila ({@code SELECT … FOR UPDATE}) para que dos refresh simultáneos con el mismo
     * token no puedan rotarlo dos veces.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    Optional<RefreshToken> findByHashToken(String hashToken);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken t set t.revocadoEn = :cuando where t.familia = :familia and t.revocadoEn is null")
    int revocarFamilia(UUID familia, OffsetDateTime cuando);

    @Modifying
    @Query("delete from RefreshToken t where t.expiraEn < :limite")
    int eliminarExpiradosAntesDe(OffsetDateTime limite);
}
