package co.nalu.api.admin;

import static co.nalu.api.error.Validacion.validar;

import co.nalu.negocio.catalogo.SeoAdminService;
import co.nalu.negocio.catalogo.dto.SeoDtos.SeoAdmin;
import co.nalu.negocio.catalogo.dto.SeoDtos.SeoRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** RF05: metadatos SEO de productos y categorías. */
@RestController
@RequestMapping("/api/v1/admin/seo")
public class SeoAdminController {

    private final SeoAdminService seo;

    public SeoAdminController(SeoAdminService seo) {
        this.seo = seo;
    }

    @GetMapping("/productos/{id}")
    SeoAdmin producto(@PathVariable long id) {
        return seo.deProducto(id);
    }

    @PutMapping("/productos/{id}")
    SeoAdmin guardarProducto(@PathVariable long id, @RequestBody SeoRequest solicitud) {
        return seo.guardarDeProducto(id, validar(solicitud, SeoRequest.ESQUEMA));
    }

    @GetMapping("/categorias/{id}")
    SeoAdmin categoria(@PathVariable long id) {
        return seo.deCategoria(id);
    }

    @PutMapping("/categorias/{id}")
    SeoAdmin guardarCategoria(@PathVariable long id, @RequestBody SeoRequest solicitud) {
        return seo.guardarDeCategoria(id, validar(solicitud, SeoRequest.ESQUEMA));
    }
}
