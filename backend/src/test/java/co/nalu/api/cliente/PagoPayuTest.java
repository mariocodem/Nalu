package co.nalu.api.cliente;

import static co.nalu.RespuestasPayu.API_KEY;
import static co.nalu.RespuestasPayu.md5;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.nalu.ApiTestBase;
import co.nalu.RespuestasPayu;
import co.nalu.datos.entidad.EstadoPedido;
import co.nalu.datos.repositorio.PedidoRepository;
import co.nalu.datos.repositorio.VarianteProductoRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

/**
 * PayU WebCheckout (ADR 006): el pedido trae el formulario firmado y la respuesta firmada que el
 * navegador reenvía confirma o rechaza el pago, una sola vez y solo para el dueño del pedido.
 */
@TestPropertySource(properties = "nalu.pagos.pasarela=payu")
class PagoPayuTest extends ApiTestBase {

    private static final String RESPUESTA = "/api/v1/pagos/payu/respuesta";
    private static final String DIRECCION = """
            {"etiqueta":"Casa","destinatario":"Ana Pérez","departamento":"Antioquia","ciudad":"Medellín",
             "direccion":"Calle 10 # 43-12","telefono":"3001234567"}""";

    @Autowired private VarianteProductoRepository variantes;
    @Autowired private PedidoRepository pedidos;

    private String admin;

    @BeforeEach
    void preparar() throws Exception {
        crearAdministrador("admin@nalu.co");
        admin = bearer("admin@nalu.co");
    }

    @Test
    void elPedidoTraeElFormularioFirmadoDePayu() throws Exception {
        long variante = productoPublicado(admin, "ANI-U", "100000", 2).getFirst();
        var ana = clienteConCarrito("ana@nalu.co", variante, 1);

        var creado = crearPedido(ana);
        String numero = JsonPath.read(creado, "$.numero");
        String referencia = JsonPath.read(creado, "$.pago.referencia");
        Map<String, Object> campos = JsonPath.read(creado, "$.pago.campos");

        assertThat(referencia).isEqualTo(numero + "-1");
        assertThat((String) JsonPath.read(creado, "$.pago.urlPago"))
                .isEqualTo("https://sandbox.checkout.payulatam.com/ppp-web-gateway-payu/");
        // 100.000 de la pieza + 15.000 de envío
        assertThat(campos)
                .containsEntry("amount", "115000")
                .containsEntry("referenceCode", referencia)
                .containsEntry("signature", md5(API_KEY + "~508029~" + referencia + "~115000~COP"))
                .containsEntry("buyerEmail", "ana@nalu.co")
                .containsEntry("test", "1")
                .containsEntry("responseUrl", "http://localhost:4200/pago/resultado?pedido=" + numero);
    }

    @Test
    void unaRespuestaAprobadaPagaElPedidoUnaSolaVez() throws Exception {
        long variante = productoPublicado(admin, "ANI-V", "100000", 3).getFirst();
        var ana = clienteConCarrito("ana@nalu.co", variante, 2);
        var creado = crearPedido(ana);
        String numero = JsonPath.read(creado, "$.numero");
        var aprobada = RespuestasPayu.respuesta(JsonPath.read(creado, "$.pago.referencia"), centavos(creado), "4", "tx-1");

        for (int i = 0; i < 2; i++) { // el cliente recarga la página del resultado
            registrar(ana, aprobada)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.numero").value(numero))
                    .andExpect(jsonPath("$.estado").value("PAGADO"))
                    .andExpect(jsonPath("$.estadoPago").value("APROBADO"));
        }

        var v = variantes.findById(variante).orElseThrow();
        assertThat(v.getStockActual()).isEqualTo(1);
        assertThat(v.getStockReservado()).isZero();
    }

    @Test
    void unaRespuestaRechazadaLiberaLaReservaYSePuedeReintentar() throws Exception {
        long variante = productoPublicado(admin, "ANI-X", "100000", 1).getFirst();
        var ana = clienteConCarrito("ana@nalu.co", variante, 1);
        var creado = crearPedido(ana);
        String numero = JsonPath.read(creado, "$.numero");
        long centavos = centavos(creado);

        registrar(ana, RespuestasPayu.respuesta(JsonPath.read(creado, "$.pago.referencia"), centavos, "6", "tx-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"))
                .andExpect(jsonPath("$.estadoPago").value("RECHAZADO"));
        assertThat(variantes.findById(variante).orElseThrow().getStockReservado()).isZero();

        var reintento = json(ana, post("/api/v1/pedidos/" + numero + "/reintentar-pago"), "");
        String nuevaReferencia = JsonPath.read(reintento, "$.campos.referenceCode");
        assertThat(nuevaReferencia).isEqualTo(numero + "-2");
        assertThat(variantes.findById(variante).orElseThrow().getStockReservado()).isEqualTo(1);

        registrar(ana, RespuestasPayu.respuesta(nuevaReferencia, centavos, "4", "tx-3"))
                .andExpect(jsonPath("$.estado").value("PAGADO"))
                .andExpect(jsonPath("$.estadoPago").value("APROBADO"));
    }

    @Test
    void unaRespuestaPendienteMantieneLaReserva() throws Exception {
        long variante = productoPublicado(admin, "ANI-P", "100000", 1).getFirst();
        var ana = clienteConCarrito("ana@nalu.co", variante, 1);
        var creado = crearPedido(ana);

        registrar(ana, RespuestasPayu.respuesta(JsonPath.read(creado, "$.pago.referencia"), centavos(creado), "7", "tx-4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"))
                .andExpect(jsonPath("$.estadoPago").value("PENDIENTE"));
        assertThat(variantes.findById(variante).orElseThrow().getStockReservado()).isEqualTo(1);
    }

    @Test
    void unaRespuestaAlteradaSeRechazaSinTocarNada() throws Exception {
        long variante = productoPublicado(admin, "ANI-F", "100000", 2).getFirst();
        var ana = clienteConCarrito("ana@nalu.co", variante, 1);
        var creado = crearPedido(ana);
        var rechazada = RespuestasPayu.respuesta(JsonPath.read(creado, "$.pago.referencia"), centavos(creado), "6", "tx-5");

        registrar(ana, rechazada.replace("\"transactionState\":\"6\"", "\"transactionState\":\"4\""))
                .andExpect(status().isUnauthorized());

        assertThat(pedidos.findByNumeroPedido(JsonPath.read(creado, "$.numero")).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.PENDIENTE_PAGO);
        assertThat(variantes.findById(variante).orElseThrow().getStockReservado()).isEqualTo(1);
    }

    @Test
    void soloElDuenoDelPedidoRegistraLaRespuesta() throws Exception {
        long variante = productoPublicado(admin, "ANI-D", "100000", 2).getFirst();
        var ana = clienteConCarrito("ana@nalu.co", variante, 1);
        var creado = crearPedido(ana);
        var aprobada = RespuestasPayu.respuesta(JsonPath.read(creado, "$.pago.referencia"), centavos(creado), "4", "tx-6");
        crearCliente("eva@nalu.co");

        registrar(bearer("eva@nalu.co"), aprobada).andExpect(status().isNotFound());
        mvc.perform(post(RESPUESTA).contentType(MediaType.APPLICATION_JSON).content(aprobada))
                .andExpect(status().isUnauthorized());
        assertThat(pedidos.findByNumeroPedido(JsonPath.read(creado, "$.numero")).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.PENDIENTE_PAGO);
    }

    // ── Utilidades ───────────────────────────────────────────────────────────────────────────

    private ResultActions registrar(String token, String respuesta) throws Exception {
        return mvc.perform(post(RESPUESTA).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(respuesta));
    }

    private String clienteConCarrito(String correo, long variante, int cantidad) throws Exception {
        crearCliente(correo);
        var token = bearer(correo);
        json(token, post("/api/v1/carrito/items"), "{\"idVariante\":%d,\"cantidad\":%d}".formatted(variante, cantidad));
        return token;
    }

    private String crearPedido(String token) throws Exception {
        long direccion = idDe(json(token, post("/api/v1/cuenta/direcciones"), DIRECCION));
        return json(token, post("/api/v1/pedidos"), "{\"idDireccion\":%d}".formatted(direccion));
    }

    private static long centavos(String pedidoCreado) {
        return ((Number) JsonPath.read(pedidoCreado, "$.pago.montoCentavos")).longValue();
    }
}
