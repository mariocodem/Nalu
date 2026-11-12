package co.nalu.api.publico;

import co.nalu.negocio.catalogo.MaterialService;
import co.nalu.negocio.catalogo.dto.MaterialDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MaterialController {

    private final MaterialService materiales;

    public MaterialController(MaterialService materiales) {
        this.materiales = materiales;
    }

    @GetMapping("/api/v1/materiales")
    List<MaterialDto> listar() {
        return materiales.listar();
    }
}
