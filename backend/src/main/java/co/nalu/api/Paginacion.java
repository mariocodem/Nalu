package co.nalu.api;

import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Convierte {@code page}, {@code size} y {@code sort=campo,asc|desc} en un {@link Pageable} seguro:
 * solo se ordena por campos permitidos y el tamaño se limita a {@value #TAMANO_MAXIMO}.
 */
public final class Paginacion {

    public static final int TAMANO_MAXIMO = 100;

    private Paginacion() {}

    public static Pageable de(int pagina, int tamano, String orden, Set<String> camposPermitidos, Sort porDefecto) {
        var sort = porDefecto;
        if (orden != null && !orden.isBlank()) {
            var partes = orden.split(",");
            if (camposPermitidos.contains(partes[0])) {
                var direccion = partes.length > 1 && partes[1].equalsIgnoreCase("desc")
                        ? Sort.Direction.DESC : Sort.Direction.ASC;
                sort = Sort.by(direccion, partes[0]);
            }
        }
        return PageRequest.of(Math.max(pagina, 0), Math.clamp(tamano, 1, TAMANO_MAXIMO), sort);
    }
}
