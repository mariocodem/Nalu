package co.nalu.negocio.pedidos;

import co.nalu.datos.repositorio.PedidoRepository;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Número de pedido {@code NALU-XXXXXX} (ADR 003): aleatorio, sin caracteres que se confundan al
 * dictarlo (0/O, 1/I/L) y único. 31^6 ≈ 887 millones de combinaciones.
 */
@Component
class GeneradorNumeroPedido {

    static final String ALFABETO = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    static final String PATRON = "^NALU-[" + ALFABETO + "]{6}$";

    private final PedidoRepository pedidos;
    private final SecureRandom aleatorio = new SecureRandom();

    GeneradorNumeroPedido(PedidoRepository pedidos) {
        this.pedidos = pedidos;
    }

    String nuevo() {
        String numero;
        do {
            var sb = new StringBuilder("NALU-");
            for (int i = 0; i < 6; i++) {
                sb.append(ALFABETO.charAt(aleatorio.nextInt(ALFABETO.length())));
            }
            numero = sb.toString();
        } while (pedidos.existsByNumeroPedido(numero));
        return numero;
    }
}
