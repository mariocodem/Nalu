package co.nalu.api.webhook;

import co.nalu.negocio.pagos.PagoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Eventos de Wompi (plan §6.4). Público y sin sesión: lo protege la firma del evento, que se
 * verifica en la capa de integración. Responde 200 a todo evento válido (también a los repetidos),
 * porque la pasarela reintenta mientras no lo reciba.
 */
@RestController
public class WompiWebhookController {

    private final PagoService pagos;

    public WompiWebhookController(PagoService pagos) {
        this.pagos = pagos;
    }

    @PostMapping("/api/v1/webhooks/wompi")
    ResponseEntity<Void> recibir(@RequestBody String cuerpo) {
        pagos.procesarEvento(cuerpo);
        return ResponseEntity.ok().build();
    }
}
