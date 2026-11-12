package co.nalu.datos.repositorio;

import co.nalu.datos.entidad.EstadoProducto;
import co.nalu.datos.entidad.Producto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    List<Producto> findDistinctByMateriales_Material_Id(Long idMaterial);

    @EntityGraph(attributePaths = {"categoria", "imagenes"})
    Optional<Producto> findBySlugAndEstado(String slug, EstadoProducto estado);

    List<Producto> findByEstado(EstadoProducto estado);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);
}
