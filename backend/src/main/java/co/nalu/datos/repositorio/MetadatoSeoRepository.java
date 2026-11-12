package co.nalu.datos.repositorio;

import co.nalu.datos.entidad.MetadatoSeo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetadatoSeoRepository extends JpaRepository<MetadatoSeo, Long> {

    Optional<MetadatoSeo> findByProductoId(Long idProducto);

    Optional<MetadatoSeo> findByCategoriaId(Long idCategoria);
}
