package co.nalu.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.nalu.ApiTestBase;
import co.nalu.EventosWompi;
import co.nalu.datos.repositorio.VarianteProductoRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Fase 5. Criterio de aceptación: un pedido recorre todos sus estados y el reporte de ventas por
 * material lo refleja. También inventario manual, alertas, reseñas y cancelación con reintegro.
 */
class OperacionTest extends ApiTestBase {

    @Autowired private VarianteProductoRepository variantes;

    private String admin;

    @BeforeEach
    void preparar() throws Exception {
        crearAdministrador("admin@nalu.co");
        admin = bearer("admin@nalu.co");
    }

    @Test
    void criterioDeAceptacionUnPedidoRecorreSuCicloYElReporteLoRefleja() throws Exception {
        var ids = productoPublicado(admin, "ANI-O", "100000", 5);
        var ana = cliente("ana@nalu.co");
        var numero = pedidoPagado(ana, ids.getFirst(), 2);
        var base = "/api/v1/admin/pedidos/" + numero;

        enviar(patch(base + "/estado"), "{\"estado\":\"PAGADO\"}", 409);
        enviar(patch(base + "/estado"), "{\"estado\":\"ENVIADO\"}", 409);
        enviar(patch(base + "/estado"), "{\"estado\":\"EN_PREPARACION\",\"nota\":\"Empacando\"}", 200);
        var enviado = enviar(post(base + "/envio"), "{\"transportadora\":\"Servientrega\",\"numeroGuia\":\"SV-123\"}", 200);
        assertThat((String) JsonPath.read(enviado, "$.pedido.estado")).isEqualTo("ENVIADO");
        assertThat((String) JsonPath.read(enviado, "$.pedido.envio.numeroGuia")).isEqualTo("SV-123");
        enviar(patch(base + "/estado"), "{\"estado\":\"ENTREGADO\"}", 200);

        mvc.perform(get("/api/v1/cuenta/pedidos/" + numero).header(HttpHeaders.AUTHORIZATION, ana))
                .andExpect(jsonPath("$.estado").value("ENTREGADO"))
                .andExpect(jsonPath("$.envio.fechaEntrega").isNotEmpty())
                .andExpect(jsonPath("$.historial[*].estado").value(contains("PENDIENTE_PAGO", "PAGADO", "EN_PREPARACION",
                        "ENVIADO", "ENTREGADO")));
        enviar(patch(base + "/estado"), "{\"estado\":\"CANCELADO\",\"nota\":\"x\"}", 409);

        var hoy = LocalDate.now().toString();
        mvc.perform(get("/api/v1/admin/reportes/ventas").param("desde", hoy).param("hasta", hoy)
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.filas[0][0]").value("Plata ANI-O 925"))
                .andExpect(jsonPath("$.filas[0][1]").value(2))
                .andExpect(jsonPath("$.filas[0][2]").value(200000))
                .andExpect(jsonPath("$.resumen.pedidos").value(1));
        mvc.perform(get("/api/v1/admin/reportes/rotacion").param("desde", hoy).param("hasta", hoy).param("formato", "csv")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString(".csv")))
                .andExpect(content().string(containsString("SKU,Producto,Unidades,Valor")))
                .andExpect(content().string(containsString("ANI-O,Pieza ANI-O,2,200000")));
    }

    @Test
    void cancelarUnPedidoPagadoExigeMotivoYReintegraLasUnidades() throws Exception {
        long variante = productoPublicado(admin, "COL-O", "50000", 3).getFirst();
        var numero = pedidoPagado(cliente("ana@nalu.co"), variante, 2);
        assertThat(variantes.findById(variante).orElseThrow().getStockActual()).isEqualTo(1);

        enviar(patch("/api/v1/admin/pedidos/" + numero + "/estado"), "{\"estado\":\"CANCELADO\"}", 409);
        enviar(patch("/api/v1/admin/pedidos/" + numero + "/estado"),
                "{\"estado\":\"CANCELADO\",\"nota\":\"La clienta desistió; reembolso por Wompi\"}", 200);
        assertThat(variantes.findById(variante).orElseThrow().getStockActual()).isEqualTo(3);
        mvc.perform(get("/api/v1/admin/inventario/movimientos").param("idVariante", String.valueOf(variante))
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.contenido[0].tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.contenido[0].numeroPedido").value(numero))
                .andExpect(jsonPath("$.contenido[0].responsable").value("Sistema"));
    }

    @Test
    void losMovimientosManualesNoTocanUnidadesReservadas() throws Exception {
        long variante = productoPublicado(admin, "ARE-O", "50000", 2).getFirst();
        var ana = cliente("ana@nalu.co");
        json(ana, post("/api/v1/carrito/items"), "{\"idVariante\":%d,\"cantidad\":2}".formatted(variante));
        json(ana, post("/api/v1/pedidos"), "{\"idDireccion\":%d}".formatted(direccion(ana)));

        var url = "/api/v1/admin/inventario/movimientos";
        enviar(post(url), "{\"idVariante\":%d,\"tipo\":\"SALIDA\",\"cantidad\":1,\"motivo\":\"Pieza dañada\"}".formatted(variante), 409);
        enviar(post(url), "{\"idVariante\":%d,\"tipo\":\"ENTRADA\",\"cantidad\":5,\"motivo\":\"Lote nuevo\"}".formatted(variante), 201);
        var salida = enviar(post(url), "{\"idVariante\":%d,\"tipo\":\"SALIDA\",\"cantidad\":1,\"motivo\":\"Pieza dañada\"}".formatted(variante), 201);
        assertThat((Integer) JsonPath.read(salida, "$.cantidad")).isEqualTo(-1);
        enviar(post(url), "{\"idVariante\":%d,\"tipo\":\"ENTRADA\",\"cantidad\":-1,\"motivo\":\"x\"}".formatted(variante), 400);
        var v = variantes.findById(variante).orElseThrow();
        assertThat(v.getStockActual()).isEqualTo(6);
        assertThat(v.getStockReservado()).isEqualTo(2);

        mvc.perform(get("/api/v1/admin/inventario/alertas").param("umbral", "4").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$[0].sku").value("ARE-O-0"))
                .andExpect(jsonPath("$[0].disponible").value(4));
    }

    @Test
    void soloQuienRecibioLaPiezaLaResenaYSePublicaTrasModerar() throws Exception {
        long variante = productoPublicado(admin, "PUL-O", "80000", 3).getFirst();
        var ana = cliente("ana@nalu.co");
        var numero = pedidoPagado(ana, variante, 1);
        var resena = "{\"calificacion\":5,\"comentario\":\"Hermosa y bien empacada\"}";
        var slug = "/api/v1/productos/pieza-pul-o/resenas";

        mvc.perform(post(slug).header(HttpHeaders.AUTHORIZATION, ana).contentType(MediaType.APPLICATION_JSON).content(resena))
                .andExpect(status().isConflict());
        entregar(numero);
        mvc.perform(post(slug).header(HttpHeaders.AUTHORIZATION, ana).contentType(MediaType.APPLICATION_JSON).content(resena))
                .andExpect(status().isCreated());
        mvc.perform(post(slug).header(HttpHeaders.AUTHORIZATION, ana).contentType(MediaType.APPLICATION_JSON).content(resena))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/productos/pieza-pul-o")).andExpect(jsonPath("$.resenas.total").value(0));

        var pendientes = mvc.perform(get("/api/v1/admin/resenas").param("estado", "PENDIENTE").header(HttpHeaders.AUTHORIZATION, admin))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(pendientes, "$.contenido[0].id")).longValue();
        enviar(patch("/api/v1/admin/resenas/" + id), "{\"estado\":\"APROBADA\"}", 200);

        mvc.perform(get("/api/v1/productos/pieza-pul-o"))
                .andExpect(jsonPath("$.resenas.total").value(1))
                .andExpect(jsonPath("$.resenas.promedio").value(5.0))
                .andExpect(jsonPath("$.resenas.resenas[0].autor").value("Ana P."))
                .andExpect(jsonPath("$.seo.jsonLd.aggregateRating.ratingValue").value(5.0));
    }

    /** RNF15: quién, cuándo y qué hizo en el panel, y cada cambio de precio con su valor anterior. */
    @Test
    void lasAccionesDelPanelYLosCambiosDePrecioQuedanAuditados() throws Exception {
        long categoria = idDe(json(admin, post("/api/v1/admin/categorias"), "{\"nombre\":\"Aretes\"}"));
        long producto = idDe(json(admin, post("/api/v1/admin/productos"),
                "{\"sku\":\"ARE-A\",\"nombre\":\"Arete\",\"idCategoria\":%d,\"precioBase\":100000}".formatted(categoria)));
        json(admin, org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/admin/productos/" + producto),
                "{\"sku\":\"ARE-A\",\"nombre\":\"Arete\",\"idCategoria\":%d,\"precioBase\":120000}".formatted(categoria));

        mvc.perform(get("/api/v1/admin/auditoria").param("recurso", "productos").param("idRecurso", String.valueOf(producto))
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.contenido[*].accion").value(org.hamcrest.Matchers.hasItems(
                        "PUT /api/v1/admin/productos/" + producto, "CAMBIO_PRECIO")))
                .andExpect(jsonPath("$.contenido[?(@.accion == 'CAMBIO_PRECIO')].detalle")
                        .value(org.hamcrest.Matchers.hasItem("Precio base 100000.00 → 120000")))
                .andExpect(jsonPath("$.contenido[?(@.accion != 'CAMBIO_PRECIO')].usuario")
                        .value(org.hamcrest.Matchers.hasItem("admin@nalu.co")));
    }

    // ── Utilidades ───────────────────────────────────────────────────────────────────────────

    private String cliente(String correo) throws Exception {
        crearCliente(correo);
        return bearer(correo);
    }

    private long direccion(String token) throws Exception {
        return idDe(json(token, post("/api/v1/cuenta/direcciones"), """
                {"etiqueta":"Casa","destinatario":"Ana Pérez","departamento":"Antioquia","ciudad":"Medellín",
                 "direccion":"Calle 10 # 43-12","telefono":"3001234567"}"""));
    }

    /** Compra y pago aprobado por webhook firmado. Devuelve el número del pedido. */
    private String pedidoPagado(String cliente, long variante, int cantidad) throws Exception {
        json(cliente, post("/api/v1/carrito/items"), "{\"idVariante\":%d,\"cantidad\":%d}".formatted(variante, cantidad));
        var creado = json(cliente, post("/api/v1/pedidos"), "{\"idDireccion\":%d}".formatted(direccion(cliente)));
        String referencia = JsonPath.read(creado, "$.pago.referencia");
        long centavos = ((Number) JsonPath.read(creado, "$.pago.montoCentavos")).longValue();
        mvc.perform(post("/api/v1/webhooks/wompi").contentType(MediaType.APPLICATION_JSON)
                .content(EventosWompi.evento("tx-" + referencia, referencia, "APPROVED", centavos))).andExpect(status().isOk());
        return JsonPath.read(creado, "$.numero");
    }

    private void entregar(String numero) throws Exception {
        var base = "/api/v1/admin/pedidos/" + numero;
        enviar(patch(base + "/estado"), "{\"estado\":\"EN_PREPARACION\"}", 200);
        enviar(post(base + "/envio"), "{\"transportadora\":\"Interrapidísimo\",\"numeroGuia\":\"IR-9\"}", 200);
        enviar(patch(base + "/estado"), "{\"estado\":\"ENTREGADO\"}", 200);
    }

    private String enviar(MockHttpServletRequestBuilder solicitud, String cuerpo, int estado) throws Exception {
        return mvc.perform(solicitud.header(HttpHeaders.AUTHORIZATION, admin).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().is(estado)).andReturn().getResponse().getContentAsString();
    }
}
