package co.nalu.api.admin;

import static co.nalu.api.error.Validacion.validar;

import co.nalu.negocio.catalogo.CategoriaService;
import co.nalu.negocio.catalogo.dto.CategoriaAdminDto;
import co.nalu.negocio.catalogo.dto.CategoriaRequest;
import co.nalu.negocio.catalogo.dto.EstadoCategoriaRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** RF03. La seguridad de /api/v1/admin/** exige el rol ADMINISTRADOR (SeguridadConfig). */
@RestController
@RequestMapping("/api/v1/admin/categorias")
public class CategoriaAdminController {

    private final CategoriaService categorias;

    public CategoriaAdminController(CategoriaService categorias) {
        this.categorias = categorias;
    }

    @GetMapping
    List<CategoriaAdminDto> arbol() {
        return categorias.arbol();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CategoriaAdminDto crear(@RequestBody CategoriaRequest solicitud) {
        return categorias.crear(validar(solicitud, CategoriaRequest.ESQUEMA));
    }

    @PutMapping("/{id}")
    CategoriaAdminDto actualizar(@PathVariable long id, @RequestBody CategoriaRequest solicitud) {
        return categorias.actualizar(id, validar(solicitud, CategoriaRequest.ESQUEMA));
    }

    @PatchMapping("/{id}/estado")
    CategoriaAdminDto cambiarEstado(@PathVariable long id, @RequestBody EstadoCategoriaRequest solicitud) {
        return categorias.cambiarEstado(id, solicitud.activa());
    }
}
