package co.nalu.datos.repositorio;

import co.nalu.datos.entidad.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    Page<Auditoria> findByRecursoAndIdRecurso(String recurso, String idRecurso, Pageable pagina);
}
