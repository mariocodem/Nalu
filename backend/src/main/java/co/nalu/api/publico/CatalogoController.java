package co.nalu.api.publico;

import co.nalu.api.Paginacion;
import co.nalu.negocio.catalogo.CatalogoPublicoService;
import co.nalu.negocio.catalogo.dto.CatalogoPublicoDtos.CategoriaDetalle;
import co.nalu.negocio.catalogo.dto.CatalogoPublicoDtos.CategoriaPublica;
import co.nalu.negocio.catalogo.dto.CatalogoPublicoDtos.FichaProducto;
import co.nalu.negocio.catalogo.dto.CatalogoPublicoDtos.FiltroCatalogo;
import co.nalu.negocio.catalogo.dto.CatalogoPublicoDtos.ProductoTarjeta;
import co.nalu.negocio.comun.Pagina;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Tienda pública, sin token (RF06, RF07). Contrato: docs/arquitectura/api-fase2.md. */
@RestController
@RequestMapping("/api/v1")
public class CatalogoController {

    /** Orden admitido: {@code ?sort=precioDesde,asc}, {@code nombre}, {@code fechaCreacion}. */
    private static final Set<String> ORDEN_PERMITIDO = Set.of("precioDesde", "nombre", "fechaCreacion");

    private final CatalogoPublicoService catalogo;

    public CatalogoController(CatalogoPublicoService catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping("/categorias")
    List<CategoriaPublica> categorias() {
        return catalogo.arbolCategorias();
    }

    @GetMapping("/categorias/{slug}")
    CategoriaDetalle categoria(@PathVariable String slug) {
        return catalogo.categoria(slug);
    }

    @GetMapping("/productos")
    Pagina<ProductoTarjeta> productos(FiltroCatalogo filtro,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "24") int size,
                                      @RequestParam(required = false) String sort) {
        var pagina = Paginacion.de(page, size, sort, ORDEN_PERMITIDO, Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        return catalogo.buscar(filtro, pagina);
    }

    @GetMapping("/productos/{slug}")
    FichaProducto ficha(@PathVariable String slug) {
        return catalogo.ficha(slug);
    }

    /** Rutas indexables para el sitemap que genera el servidor SSR. */
    @GetMapping("/seo/rutas")
    Map<String, List<String>> rutas() {
        return Map.of("productos", catalogo.slugsPublicados(), "categorias", catalogo.slugsCategorias());
    }
}
