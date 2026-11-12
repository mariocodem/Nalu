package co.nalu.api.admin;

import static co.nalu.api.error.Validacion.validar;

import co.nalu.negocio.catalogo.MaterialService;
import co.nalu.negocio.catalogo.dto.MaterialDto;
import co.nalu.negocio.catalogo.dto.MaterialRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/materiales")
public class MaterialAdminController {

    private final MaterialService materiales;

    public MaterialAdminController(MaterialService materiales) {
        this.materiales = materiales;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    MaterialDto crear(@RequestBody MaterialRequest solicitud) {
        return materiales.crear(validar(solicitud, MaterialRequest.ESQUEMA));
    }

    @PutMapping("/{id}")
    MaterialDto actualizar(@PathVariable long id, @RequestBody MaterialRequest solicitud) {
        return materiales.actualizar(id, validar(solicitud, MaterialRequest.ESQUEMA));
    }
}
