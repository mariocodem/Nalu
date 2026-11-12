package co.nalu.api.admin;

import static co.nalu.api.error.Validacion.validar;

import co.nalu.api.Paginacion;
import co.nalu.api.UsuarioActual;
import co.nalu.api.error.EntradaInvalidaException;
import co.nalu.api.error.ErrorCampo;
import co.nalu.negocio.catalogo.ProductoAdminService;
import co.nalu.negocio.catalogo.dto.EstadoProductoRequest;
import co.nalu.negocio.catalogo.dto.FiltroProductos;
import co.nalu.negocio.catalogo.dto.ImagenRequest;
import co.nalu.negocio.catalogo.dto.MaterialProductoRequest;
import co.nalu.negocio.catalogo.dto.ProductoAdminDto;
import co.nalu.negocio.catalogo.dto.ProductoRequest;
import co.nalu.negocio.catalogo.dto.ProductoResumenDto;
import co.nalu.negocio.catalogo.dto.VarianteNuevaRequest;
import co.nalu.negocio.catalogo.dto.VarianteRequest;
import co.nalu.negocio.comun.Pagina;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** RF04 / CU-11. Contrato: docs/arquitectura/api-fase1.md §4. */
@RestController
@RequestMapping("/api/v1/admin/productos")
public class ProductoAdminController {

    private static final Set<String> ORDEN_PERMITIDO = Set.of("nombre", "sku", "precioBase", "fechaCreacion", "estado");

    private final ProductoAdminService productos;

    public ProductoAdminController(ProductoAdminService productos) {
        this.productos = productos;
    }

    @GetMapping
    Pagina<ProductoResumenDto> listar(FiltroProductos filtro,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size,
                                      @RequestParam(required = false) String sort) {
        var pagina = Paginacion.de(page, size, sort, ORDEN_PERMITIDO, Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        return productos.listar(filtro, pagina);
    }

    @GetMapping("/{id}")
    ProductoAdminDto obtener(@PathVariable long id) {
        return productos.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ProductoAdminDto crear(@RequestBody ProductoRequest solicitud) {
        return productos.crear(validar(solicitud, ProductoRequest.ESQUEMA));
    }

    @PutMapping("/{id}")
    ProductoAdminDto actualizar(@PathVariable long id, @RequestBody ProductoRequest solicitud) {
        return productos.actualizar(id, validar(solicitud, ProductoRequest.ESQUEMA));
    }

    @PutMapping("/{id}/materiales")
    ProductoAdminDto materiales(@PathVariable long id, @RequestBody List<MaterialProductoRequest> solicitud) {
        var errores = new ArrayList<ErrorCampo>();
        for (int i = 0; i < solicitud.size(); i++) {
            int indice = i;
            MaterialProductoRequest.ESQUEMA.validate(solicitud.get(i)).forEach(v ->
                    errores.add(new ErrorCampo("[" + indice + "]." + v.name(), v.message())));
        }
        if (solicitud.stream().map(MaterialProductoRequest::idMaterial).distinct().count() < solicitud.size()) {
            errores.add(new ErrorCampo("", "Un material no puede repetirse en el mismo producto"));
        }
        if (!errores.isEmpty()) {
            throw new EntradaInvalidaException(errores);
        }
        return productos.reemplazarMateriales(id, solicitud);
    }

    @PostMapping("/{id}/variantes")
    @ResponseStatus(HttpStatus.CREATED)
    ProductoAdminDto agregarVariante(@PathVariable long id, @RequestBody VarianteNuevaRequest solicitud,
                                     @AuthenticationPrincipal Jwt jwt) {
        return productos.agregarVariante(id, validar(solicitud, VarianteNuevaRequest.ESQUEMA), UsuarioActual.id(jwt));
    }

    @PutMapping("/{id}/variantes/{idVariante}")
    ProductoAdminDto actualizarVariante(@PathVariable long id, @PathVariable long idVariante,
                                        @RequestBody VarianteRequest solicitud) {
        return productos.actualizarVariante(id, idVariante, validar(solicitud, VarianteRequest.ESQUEMA));
    }

    @PostMapping(path = "/{id}/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ProductoAdminDto agregarImagen(@PathVariable long id, @RequestPart("archivo") MultipartFile archivo,
                                   @RequestPart("textoAlternativo") String textoAlternativo) {
        var texto = textoAlternativo == null ? "" : textoAlternativo.strip();
        if (texto.isEmpty() || texto.length() > 160) {
            throw new EntradaInvalidaException(List.of(new ErrorCampo("textoAlternativo",
                    "El texto alternativo es obligatorio y admite como máximo 160 caracteres")));
        }
        try {
            return productos.agregarImagen(id, archivo.getBytes(), texto);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la imagen subida", e);
        }
    }

    @PatchMapping("/{id}/imagenes/{idImagen}")
    ProductoAdminDto actualizarImagen(@PathVariable long id, @PathVariable long idImagen,
                                      @RequestBody ImagenRequest solicitud) {
        return productos.actualizarImagen(id, idImagen, validar(solicitud, ImagenRequest.ESQUEMA));
    }

    @DeleteMapping("/{id}/imagenes/{idImagen}")
    ProductoAdminDto eliminarImagen(@PathVariable long id, @PathVariable long idImagen) {
        return productos.eliminarImagen(id, idImagen);
    }

    @PatchMapping("/{id}/estado")
    ProductoAdminDto cambiarEstado(@PathVariable long id, @RequestBody EstadoProductoRequest solicitud) {
        if (solicitud.estado() == null) {
            throw new EntradaInvalidaException(List.of(new ErrorCampo("estado", "El estado es obligatorio")));
        }
        return productos.cambiarEstado(id, solicitud);
    }
}
