package co.nalu.api.cliente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.nalu.ApiTestBase;
import co.nalu.datos.entidad.EstadoPago;
import co.nalu.datos.entidad.EstadoPedido;
import co.nalu.datos.entidad.TipoMovimiento;
import co.nalu.datos.repositorio.MovimientoInventarioRepository;
import co.nalu.datos.repositorio.PagoRepository;
import co.nalu.datos.repositorio.PedidoRepository;
import co.nalu.datos.repositorio.PromocionRepository;
import co.nalu.datos.repositorio.VarianteProductoRepository;
import co.nalu.negocio.pedidos.TareasPedidos;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Fases 3 y 4: carrito, checkout atómico con reserva, concurrencia sobre la última unidad,
 * webhook firmado e idempotente, reintento y tareas programadas (plan §6.3–§6.5 y §14).
 */
class CompraTest extends ApiTestBase {

    private static final String SECRETO_EVENTOS = "test_events_nalu";
    private static final String DIRECCION = """
            {"etiqueta":"Casa","destinatario":"Ana Pérez","departamento":"Antioquia","ciudad":"Medellín",
             "direccion":"Calle 10 # 43-12","telefono":"3001234567"}""";

    @Autowired private VarianteProductoRepository variantes;
    @Autowired private PedidoRepository pedidos;
    @Autowired private PagoRepository pagos;
    @Autowired private PromocionRepository promociones;
    @Autowired private MovimientoInventarioRepository movimientos;
    @Autowired private TareasPedidos tareas;
    @Autowired private EntityManager em;
    @Autowired private TransactionTemplate transaccion;

    private String admin;

    @BeforeEach
    void preparar() throws Exception {
        crearAdministrador("admin@nalu.co");
        admin = bearer("admin@nalu.co");
    }

    // ── Carrito (RF08) ───────────────────────────────────────────────────────────────────────

    @Test
    void elCarritoDelVisitantePasaALaCuentaAlIniciarSesion() throws Exception {
        long variante = productoPublicado(admin, "ANI-C", "100000", 5).getFirst();

        var r = mvc.perform(post("/api/v1/carrito/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idVariante\":%d,\"cantidad\":2}".formatted(variante)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(200000))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(r, "$.token");
        long item = ((Number) JsonPath.read(r, "$.items[0].id")).longValue();

        mvc.perform(post("/api/v1/carrito/items").header("X-Cart-Token", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idVariante\":%d,\"cantidad\":4}".formatted(variante)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Solo quedan 5 unidades de esta pieza"));
        mvc.perform(patch("/api/v1/carrito/items/" + item).header("X-Cart-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":3}"))
                .andExpect(jsonPath("$.items[0].cantidad").value(3))
                .andExpect(jsonPath("$.items[0].maximo").value(5));

        crearCliente("ana@nalu.co");
        var ana = bearer("ana@nalu.co");
        mvc.perform(post("/api/v1/carrito/fusionar").header(HttpHeaders.AUTHORIZATION, ana).header("X-Cart-Token", token))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.cantidadTotal").value(3));
        mvc.perform(get("/api/v1/carrito").header(HttpHeaders.AUTHORIZATION, ana))
                .andExpect(jsonPath("$.items.length()").value(1));
        mvc.perform(get("/api/v1/carrito").header("X-Cart-Token", token))
                .andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(delete("/api/v1/carrito/items/" + item).header(HttpHeaders.AUTHORIZATION, ana))
                .andExpect(jsonPath("$.cantidadTotal").value(0));
    }

    // ── Cotización y pedido (RF09–RF11) ──────────────────────────────────────────────────────

    @Test
    void laCotizacionAplicaCuponYEnvioGratisDesdeElMinimo() throws Exception {
        long variante = productoPublicado(admin, "COL-C", "120000", 10).getFirst();
        json(admin, post("/api/v1/admin/promociones"), """
                {"codigo":"BIENVENIDA","tipo":"PORCENTAJE","valor":10,"fechaInicio":"2026-01-01",
                 "fechaFin":"2099-12-31","usosMaximos":100,"activa":true}""");
        var ana = clienteConCarrito("ana@nalu.co", variante, 1);

        mvc.perform(post("/api/v1/checkout/cotizar").header(HttpHeaders.AUTHORIZATION, ana)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"codigoCupon\":\"bienvenida\"}"))
                .andExpect(jsonPath("$.subtotal").value(120000))
                .andExpect(jsonPath("$.descuento").value(12000))
                .andExpect(jsonPath("$.costoEnvio").value(15000))
                .andExpect(jsonPath("$.total").value(123000))
                .andExpect(jsonPath("$.cupon.aplicado").value(true));

        json(ana, patch("/api/v1/carrito/items/" + itemDe(ana)), "{\"cantidad\":2}");
        mvc.perform(post("/api/v1/checkout/cotizar").header(HttpHeaders.AUTHORIZATION, ana)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"codigoCupon\":\"NOEXISTE\"}"))
                .andExpect(jsonPath("$.subtotal").value(240000))
                .andExpect(jsonPath("$.costoEnvio").value(0))
                .andExpect(jsonPath("$.cupon.aplicado").value(false))
                .andExpect(jsonPath("$.cupon.mensaje").value("El cupón no existe"));
    }

    @Test
    void crearElPedidoReservaConvierteElCarritoYPreparaElPago() throws Exception {
        long variante = productoPublicado(admin, "ARE-P", "50000", 3).getFirst();
        var ana = clienteConCarrito("ana@nalu.co", variante, 2);

        var creado = mvc.perform(post("/api/v1/pedidos").header(HttpHeaders.AUTHORIZATION, ana)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"idDireccion\":%d}".formatted(direccion(ana))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value(matchesPattern("^NALU-[2-9A-HJKMNP-Z]{6}$")))
                .andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"))
                .andExpect(jsonPath("$.total").value(115000))
                .andExpect(jsonPath("$.pago.montoCentavos").value(11500000))
                .andExpect(jsonPath("$.pago.urlPago").value(containsString("&signature:integrity=")))
                .andReturn().getResponse().getContentAsString();
        String numero = JsonPath.read(creado, "$.numero");
        String referencia = JsonPath.read(creado, "$.pago.referencia");
        String url = JsonPath.read(creado, "$.pago.urlPago");
        // Firma de integridad de Wompi: SHA-256(referencia + centavos + moneda + secreto).
        assertThat(url).contains(sha256(referencia + "11500000COP" + "test_integrity_nalu"));

        var v = variantes.findById(variante).orElseThrow();
        assertThat(v.getStockActual()).isEqualTo(3);
        assertThat(v.getStockReservado()).isEqualTo(2);
        mvc.perform(get("/api/v1/carrito").header(HttpHeaders.AUTHORIZATION, ana)).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(get("/api/v1/cuenta/pedidos/" + numero).header(HttpHeaders.AUTHORIZATION, ana))
                .andExpect(jsonPath("$.lineas[0].cantidad").value(2))
                .andExpect(jsonPath("$.estadoPago").value("INICIADO"))
                .andExpect(jsonPath("$.puedePagar").value(true));
        crearCliente("eva@nalu.co");
        mvc.perform(get("/api/v1/cuenta/pedidos/" + numero).header(HttpHeaders.AUTHORIZATION, bearer("eva@nalu.co")))
                .andExpect(status().isNotFound());
    }

    /** RNF10: si una línea no alcanza, no se crea nada ni se reserva nada. */
    @Test
    void siUnaLineaNoAlcanzaNoQuedaNadaAMedias() throws Exception {
        var ids = productoPublicado(admin, "PUL-A", "50000", 5, 1);
        var ana = clienteConCarrito("ana@nalu.co", ids.get(0), 2);
        json(ana, post("/api/v1/carrito/items"), "{\"idVariante\":%d,\"cantidad\":1}".formatted(ids.get(1)));
        long idDireccion = direccion(ana);
        // Otra compra se lleva la única unidad de la segunda variante.
        var eva = clienteConCarrito("eva@nalu.co", ids.get(1), 1);
        json(eva, post("/api/v1/pedidos"), "{\"idDireccion\":%d}".formatted(direccion(eva)));

        mvc.perform(post("/api/v1/pedidos").header(HttpHeaders.AUTHORIZATION, ana)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"idDireccion\":%d}".formatted(idDireccion)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.faltantes[0]").value(containsString("agotada")));

        assertThat(pedidos.count()).isEqualTo(1);
        assertThat(variantes.findById(ids.get(0)).orElseThrow().getStockReservado()).isZero();
        mvc.perform(get("/api/v1/carrito").header(HttpHeaders.AUTHORIZATION, ana)).andExpect(jsonPath("$.items.length()").value(2));
    }

    /** RNF11 y criterio de aceptación de la Fase 3: dos compras de la última unidad, solo una gana. */
    @Test
    void dosComprasSimultaneasDeLaUltimaUnidadSoloUnaObtieneLaReserva() throws Exception {
        long variante = productoPublicado(admin, "ANI-U", "300000", 1).getFirst();
        var ana = clienteConCarrito("ana@nalu.co", variante, 1);
        var eva = clienteConCarrito("eva@nalu.co", variante, 1);
        var cuerpos = List.of("{\"idDireccion\":%d}".formatted(direccion(ana)), "{\"idDireccion\":%d}".formatted(direccion(eva)));
        var tokens = List.of(ana, eva);

        var salida = new CountDownLatch(1);
        var tareasCompra = new ArrayList<Callable<Integer>>();
        for (int i = 0; i < 2; i++) {
            int indice = i;
            tareasCompra.add(() -> {
                salida.await();
                return mvc.perform(post("/api/v1/pedidos").header(HttpHeaders.AUTHORIZATION, tokens.get(indice))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpos.get(indice))).andReturn().getResponse().getStatus();
            });
        }
        try (var hilos = Executors.newFixedThreadPool(2)) {
            var futuros = tareasCompra.stream().map(hilos::submit).toList();
            salida.countDown();
            var estados = new ArrayList<Integer>();
            for (var f : futuros) {
                estados.add(f.get());
            }
            assertThat(estados).containsExactlyInAnyOrder(201, 409);
        }
        var v = variantes.findById(variante).orElseThrow();
        assertThat(v.getStockReservado()).isEqualTo(1);
        assertThat(pedidos.count()).isEqualTo(1);
    }

    // ── Pagos (RF12, plan §6.4) ──────────────────────────────────────────────────────────────

    @Test
    void unPagoAprobadoDescuentaElInventarioUnaSolaVez() throws Exception {
        long variante = productoPublicado(admin, "ANI-W", "100000", 4).getFirst();
        json(admin, post("/api/v1/admin/promociones"), """
                {"codigo":"UNO","tipo":"VALOR_FIJO","valor":5000,"fechaInicio":"2026-01-01","fechaFin":"2099-12-31",
                 "usosMaximos":1,"activa":true}""");
        var ana = clienteConCarrito("ana@nalu.co", variante, 3);
        var creado = json(ana, post("/api/v1/pedidos"), "{\"idDireccion\":%d,\"codigoCupon\":\"UNO\"}".formatted(direccion(ana)));
        String referencia = JsonPath.read(creado, "$.pago.referencia");
        long centavos = ((Number) JsonPath.read(creado, "$.pago.montoCentavos")).longValue();
        String evento = evento("tx-1", referencia, "APPROVED", centavos);

        for (int i = 0; i < 2; i++) { // el segundo es el reintento de Wompi
            mvc.perform(post("/api/v1/webhooks/wompi").contentType(MediaType.APPLICATION_JSON).content(evento))
                    .andExpect(status().isOk());
        }

        var pedido = pedidos.findByNumeroPedido(JsonPath.read(creado, "$.numero")).orElseThrow();
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PAGADO);
        var v = variantes.findById(variante).orElseThrow();
        assertThat(v.getStockActual()).isEqualTo(1);
        assertThat(v.getStockReservado()).isZero();
        assertThat(movimientos.findByVarianteIdOrderByFechaAsc(variante))
                .filteredOn(m -> m.getTipo() == TipoMovimiento.SALIDA).singleElement()
                .satisfies(m -> assertThat(m.getCantidad()).isEqualTo(-3));
        assertThat(promociones.findByCodigo("UNO").orElseThrow().getUsosActuales()).isEqualTo(1);
        assertThat(pagos.findByReferencia(referencia).orElseThrow().getEstado()).isEqualTo(EstadoPago.APROBADO);
    }

    @Test
    void unaFirmaAlteradaSeRechazaSinTocarNada() throws Exception {
        long variante = productoPublicado(admin, "ANI-F", "100000", 2).getFirst();
        var ana = clienteConCarrito("ana@nalu.co", variante, 1);
        var creado = json(ana, post("/api/v1/pedidos"), "{\"idDireccion\":%d}".formatted(direccion(ana)));
        String referencia = JsonPath.read(creado, "$.pago.referencia");
        var alterado = evento("tx-2", referencia, "APPROVED", 1).replace("\"amount_in_cents\":1", "\"amount_in_cents\":11500000");

        mvc.perform(post("/api/v1/webhooks/wompi").contentType(MediaType.APPLICATION_JSON).content(alterado))
                .andExpect(status().isUnauthorized());
        assertThat(pedidos.findByNumeroPedido(JsonPath.read(creado, "$.numero")).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.PENDIENTE_PAGO);
        assertThat(variantes.findById(variante).orElseThrow().getStockActual()).isEqualTo(2);
    }

    @Test
    void unMontoDistintoNoApruebaElPedido() throws Exception {
        long variante = productoPublicado(admin, "ANI-M", "100000", 2).getFirst();
        var ana = clienteConCarrito("ana@nalu.co", variante, 1);
        var creado = json(ana, post("/api/v1/pedidos"), "{\"idDireccion\":%d}".formatted(direccion(ana)));
        String referencia = JsonPath.read(creado, "$.pago.referencia");

        mvc.perform(post("/api/v1/webhooks/wompi").contentType(MediaType.APPLICATION_JSON)
                .content(evento("tx-3", referencia, "APPROVED", 100))).andExpect(status().isOk());
        assertThat(pagos.findByReferencia(referencia).orElseThrow().getEstado()).isEqualTo(EstadoPago.ERROR);
        assertThat(pedidos.findByNumeroPedido(JsonPath.read(creado, "$.numero")).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.PENDIENTE_PAGO);
    }

    @Test
    void unPagoRechazadoLiberaLaReservaYSePuedeReintentar() throws Exception {
        long variante = productoPublicado(admin, "ANI-R", "100000", 1).getFirst();
        var ana = clienteConCarrito("ana@nalu.co", variante, 1);
        var creado = json(ana, post("/api/v1/pedidos"), "{\"idDireccion\":%d}".formatted(direccion(ana)));
        String numero = JsonPath.read(creado, "$.numero");
        String referencia = JsonPath.read(creado, "$.pago.referencia");
        long centavos = ((Number) JsonPath.read(creado, "$.pago.montoCentavos")).longValue();

        mvc.perform(post("/api/v1/webhooks/wompi").contentType(MediaType.APPLICATION_JSON)
                .content(evento("tx-4", referencia, "DECLINED", centavos))).andExpect(status().isOk());
        assertThat(variantes.findById(variante).orElseThrow().getStockReservado()).isZero();

        var reintento = json(ana, post("/api/v1/pedidos/" + numero + "/reintentar-pago"), "");
        String nuevaReferencia = JsonPath.read(reintento, "$.referencia");
        assertThat(nuevaReferencia).isEqualTo(numero + "-2");
        assertThat(variantes.findById(variante).orElseThrow().getStockReservado()).isEqualTo(1);

        mvc.perform(post("/api/v1/webhooks/wompi").contentType(MediaType.APPLICATION_JSON)
                .content(evento("tx-5", nuevaReferencia, "APPROVED", centavos))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/cuenta/pedidos/" + numero).header(HttpHeaders.AUTHORIZATION, ana))
                .andExpect(jsonPath("$.estado").value("PAGADO"))
                .andExpect(jsonPath("$.historial[*].estado").value(hasItem("PAGADO")))
                .andExpect(jsonPath("$.puedePagar").value(false));
    }

    // ── Tareas programadas (plan §6.5, ADR 003) ──────────────────────────────────────────────

    @Test
    void laReservaVencidaSeLiberaYElPedidoSinPagoSeCancelaALas24Horas() throws Exception {
        long variante = productoPublicado(admin, "ANI-T", "100000", 2).getFirst();
        var ana = clienteConCarrito("ana@nalu.co", variante, 2);
        var creado = json(ana, post("/api/v1/pedidos"), "{\"idDireccion\":%d}".formatted(direccion(ana)));
        String numero = JsonPath.read(creado, "$.numero");

        transaccion.executeWithoutResult(t -> em.createQuery(
                "update Pedido p set p.reservaExpiraEn = :antes where p.numeroPedido = :n")
                .setParameter("antes", OffsetDateTime.now().minusMinutes(1)).setParameter("n", numero).executeUpdate());
        tareas.liberarReservasVencidas();
        assertThat(variantes.findById(variante).orElseThrow().getStockReservado()).isZero();

        transaccion.executeWithoutResult(t -> em.createQuery("update Pedido p set p.fecha = :antes where p.numeroPedido = :n")
                .setParameter("antes", OffsetDateTime.now().minusHours(25)).setParameter("n", numero).executeUpdate());
        tareas.cancelarPedidosSinPago();
        assertThat(pedidos.findByNumeroPedido(numero).orElseThrow().getEstado()).isEqualTo(EstadoPedido.CANCELADO);
    }

    // ── Utilidades ───────────────────────────────────────────────────────────────────────────

    private String clienteConCarrito(String correo, long variante, int cantidad) throws Exception {
        crearCliente(correo);
        var token = bearer(correo);
        json(token, post("/api/v1/carrito/items"), "{\"idVariante\":%d,\"cantidad\":%d}".formatted(variante, cantidad));
        return token;
    }

    private long direccion(String token) throws Exception {
        return idDe(json(token, post("/api/v1/cuenta/direcciones"), DIRECCION));
    }

    private long itemDe(String token) throws Exception {
        var carrito = mvc.perform(get("/api/v1/carrito").header(HttpHeaders.AUTHORIZATION, token))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(carrito, "$.items[0].id")).longValue();
    }

    /** Evento con la estructura y la firma documentadas por Wompi. */
    private static String evento(String idTransaccion, String referencia, String estado, long centavos) {
        long timestamp = 1_762_700_000L;
        var checksum = sha256(idTransaccion + estado + centavos + timestamp + SECRETO_EVENTOS);
        return """
                {"event":"transaction.updated",
                 "data":{"transaction":{"id":"%s","amount_in_cents":%d,"reference":"%s","currency":"COP",
                         "payment_method_type":"CARD","status":"%s"}},
                 "environment":"test",
                 "signature":{"properties":["transaction.id","transaction.status","transaction.amount_in_cents"],
                              "checksum":"%s"},
                 "timestamp":%d,"sent_at":"2026-11-09T15:00:00.000Z"}"""
                .formatted(idTransaccion, centavos, referencia, estado, checksum.toUpperCase(), timestamp);
    }

    private static String sha256(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
