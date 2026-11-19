package co.nalu.api.cliente;

import co.nalu.api.UsuarioActual;
import co.nalu.negocio.pagos.PagoService;
import co.nalu.negocio.pagos.dto.PagoDtos.RespuestaRegistrada;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Respuesta de PayU que el navegador trae de vuelta (ADR 006): el frontend reenvía como JSON los
 * parámetros con los que PayU lo devolvió a la tienda. La firma se verifica en la capa de integración.
 * Contrato: docs/arquitectura/api-fase3.md §3.
 */
@RestController
@RequestMapping("/api/v1/pagos")
public class PagoController {

    private final PagoService pagos;

    public PagoController(PagoService pagos) {
        this.pagos = pagos;
    }

    @PostMapping("/payu/respuesta")
    RespuestaRegistrada registrarRespuestaPayu(@AuthenticationPrincipal Jwt jwt, @RequestBody String cuerpo) {
        return pagos.registrarRespuesta(UsuarioActual.id(jwt), cuerpo);
    }
}
