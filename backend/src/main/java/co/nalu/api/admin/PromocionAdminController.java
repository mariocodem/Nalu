package co.nalu.api.admin;

import static co.nalu.api.error.Validacion.validar;

import co.nalu.negocio.promociones.PromocionService;
import co.nalu.negocio.promociones.dto.PromocionDtos;
import co.nalu.negocio.promociones.dto.PromocionDtos.PromocionRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** RF11: cupones. */
@RestController
@RequestMapping("/api/v1/admin/promociones")
public class PromocionAdminController {

    private final PromocionService promociones;

    public PromocionAdminController(PromocionService promociones) {
        this.promociones = promociones;
    }

    @GetMapping
    List<PromocionDtos.Promocion> listar() {
        return promociones.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PromocionDtos.Promocion crear(@RequestBody PromocionRequest solicitud) {
        return promociones.crear(validar(solicitud, PromocionRequest.ESQUEMA));
    }

    @PutMapping("/{id}")
    PromocionDtos.Promocion actualizar(@PathVariable long id, @RequestBody PromocionRequest solicitud) {
        return promociones.actualizar(id, validar(solicitud, PromocionRequest.ESQUEMA));
    }
}
