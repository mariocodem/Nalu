package co.nalu.datos.repositorio;

import co.nalu.datos.entidad.Categoria;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByOrderByNombreAsc();

    Optional<Categoria> findBySlugAndActivaTrue(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsByNombreIgnoreCase(String nombre);
}
