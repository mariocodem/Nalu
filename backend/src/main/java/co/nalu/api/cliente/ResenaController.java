package co.nalu.api.cliente;

import static co.nalu.api.error.Validacion.validar;

import co.nalu.api.UsuarioActual;
import co.nalu.negocio.contenido.ResenaService;
import co.nalu.negocio.contenido.dto.ResenaDtos.ResenaRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** RF07: reseña de una pieza recibida; queda pendiente de moderación. */
@RestController
public class ResenaController {

    private final ResenaService resenas;

    public ResenaController(ResenaService resenas) {
        this.resenas = resenas;
    }

    @PostMapping("/api/v1/productos/{slug}/resenas")
    @ResponseStatus(HttpStatus.CREATED)
    void crear(@PathVariable String slug, @RequestBody ResenaRequest solicitud, @AuthenticationPrincipal Jwt jwt) {
        resenas.crear(UsuarioActual.id(jwt), slug, validar(solicitud, ResenaRequest.ESQUEMA));
    }
}
