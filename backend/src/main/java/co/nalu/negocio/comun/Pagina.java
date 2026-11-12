package co.nalu.negocio.comun;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** Página de resultados con el formato del contrato de la API (docs/arquitectura/api-fase1.md). */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public static <E, T> Pagina<T> de(Page<E> pagina, Function<E, T> mapeo) {
        return new Pagina<>(pagina.getContent().stream().map(mapeo).toList(), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }
}
