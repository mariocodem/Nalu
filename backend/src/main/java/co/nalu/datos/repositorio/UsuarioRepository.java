package co.nalu.datos.repositorio;

import co.nalu.datos.entidad.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** El correo se guarda normalizado (sin espacios y en minúsculas). */
    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);
}
