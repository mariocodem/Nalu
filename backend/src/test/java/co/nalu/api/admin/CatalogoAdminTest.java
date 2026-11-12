package co.nalu.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.nalu.ApiTestBase;
import co.nalu.datos.entidad.TipoMovimiento;
import co.nalu.datos.repositorio.MovimientoInventarioRepository;
import co.nalu.datos.repositorio.VarianteProductoRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

/** RF03, RF04 y criterio de aceptación de la Fase 1 (docs/arquitectura/api-fase1.md §5). */
class CatalogoAdminTest extends ApiTestBase {

    /** PNG válido de 1×1 píxel. */
    private static final byte[] PNG = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

    @Autowired private VarianteProductoRepository variantes;
    @Autowired private MovimientoInventarioRepository movimientos;
    @Autowired private EntityManager em;
    @Autowired private TransactionTemplate transaccion;

    private String admin;

    @BeforeEach
    void iniciarSesion() throws Exception {
        crearAdministrador("admin@nalu.co");
        admin = bearer("admin@nalu.co");
    }

    @Test
    void criterioDeAceptacionUnProductoConDosMaterialesYTresTallasSePublica() throws Exception {
        long anillos = id(enviar(post("/api/v1/admin/categorias"), "{\"nombre\":\"Anillos de plata\"}", 201));
        long plata = id(enviar(post("/api/v1/admin/materiales"),
                "{\"tipo\":\"Plata\",\"ley\":\"925\",\"requiereCertificado\":false,\"factorPrecio\":1}", 201));
        long cuarzo = id(enviar(post("/api/v1/admin/materiales"),
                "{\"tipo\":\"Cuarzo\",\"ley\":null,\"requiereCertificado\":false,\"factorPrecio\":1.25}", 201));

        long producto = id(enviar(post("/api/v1/admin/productos"), """
                {"sku":"ANI-LUNA","nombre":"Anillo Luna","descripcion":"Plata con cuarzo rosa",
                 "idCategoria":%d,"precioBase":189000}""".formatted(anillos), 201));

        // Publicar sin materiales, variantes ni imágenes: 409 con lo que falta.
        enviar(patch("/api/v1/admin/productos/" + producto + "/estado"), "{\"estado\":\"PUBLICADO\"}", 409);

        enviar(put("/api/v1/admin/productos/" + producto + "/materiales"), """
                [{"idMaterial":%d,"rol":"METAL_PRINCIPAL","pesoGramos":3.5,"quilates":null},
                 {"idMaterial":%d,"rol":"PIEDRA","pesoGramos":null,"quilates":0.8}]""".formatted(plata, cuarzo), 200);
        for (var talla : new String[] {"6", "7", "8"}) {
            enviar(post("/api/v1/admin/productos/" + producto + "/variantes"), """
                    {"sku":"ANI-LUNA-%s","talla":"%s","pesoGramos":3.5,"precioAdicional":0,"existenciaInicial":4}"""
                    .formatted(talla, talla), 201);
        }
        mvc.perform(multipart("/api/v1/admin/productos/" + producto + "/imagenes")
                        .file(new MockMultipartFile("archivo", "luna.png", "image/png", PNG))
                        .file(new MockMultipartFile("textoAlternativo", "", "text/plain",
                                "Anillo Luna de plata con cuarzo".getBytes()))
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imagenes[0].principal").value(true));

        var publicado = enviar(patch("/api/v1/admin/productos/" + producto + "/estado"), "{\"estado\":\"PUBLICADO\"}", 200);
        assertThat((String) JsonPath.read(publicado, "$.estado")).isEqualTo("PUBLICADO");
        assertThat((Integer) JsonPath.read(publicado, "$.materiales.length()")).isEqualTo(2);
        assertThat((Integer) JsonPath.read(publicado, "$.variantes.length()")).isEqualTo(3);
        assertThat((String) JsonPath.read(publicado, "$.slug")).isEqualTo("anillo-luna");

        // La imagen se sirve públicamente y la existencia inicial quedó como movimiento de inventario.
        String url = JsonPath.read(publicado, "$.imagenes[0].url");
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().bytes(PNG));
        assertThat(movimientos.findAll()).hasSize(3)
                .allSatisfy(m -> {
                    assertThat(m.getTipo()).isEqualTo(TipoMovimiento.ENTRADA);
                    assertThat(m.getCantidad()).isEqualTo(4);
                });

        mvc.perform(get("/api/v1/admin/productos").param("q", "luna").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].existenciaTotal").value(12))
                .andExpect(jsonPath("$.contenido[0].imagenPrincipal").value(url));
    }

    @Test
    void reemplazarMaterialesActualizaQuitaYAgrega() throws Exception {
        long categoria = id(enviar(post("/api/v1/admin/categorias"), "{\"nombre\":\"Collares\"}", 201));
        long plata = id(enviar(post("/api/v1/admin/materiales"),
                "{\"tipo\":\"Plata\",\"ley\":\"925\",\"factorPrecio\":1}", 201));
        long oro = id(enviar(post("/api/v1/admin/materiales"), "{\"tipo\":\"Oro\",\"ley\":\"18K\",\"factorPrecio\":1}", 201));
        long producto = id(enviar(post("/api/v1/admin/productos"),
                "{\"sku\":\"COL-1\",\"nombre\":\"Collar\",\"idCategoria\":%d,\"precioBase\":100}".formatted(categoria), 201));
        var url = "/api/v1/admin/productos/" + producto + "/materiales";

        enviar(put(url), "[{\"idMaterial\":%d,\"rol\":\"METAL_PRINCIPAL\"}]".formatted(plata), 200);
        var resultado = enviar(put(url), """
                [{"idMaterial":%d,"rol":"SECUNDARIO","pesoGramos":2},{"idMaterial":%d,"rol":"METAL_PRINCIPAL"}]"""
                .formatted(plata, oro), 200);
        assertThat((Integer) JsonPath.read(resultado, "$.materiales.length()")).isEqualTo(2);

        enviar(put(url), "[{\"idMaterial\":%d,\"rol\":\"PIEDRA\"},{\"idMaterial\":%d,\"rol\":\"PIEDRA\"}]"
                .formatted(oro, oro), 400);
    }

    @Test
    void skuYSlugRepetidosResponden409() throws Exception {
        long categoria = id(enviar(post("/api/v1/admin/categorias"), "{\"nombre\":\"Aretes\"}", 201));
        var producto = "{\"sku\":\"ARE-1\",\"nombre\":\"Arete\",\"idCategoria\":%d,\"precioBase\":100}".formatted(categoria);
        long id = id(enviar(post("/api/v1/admin/productos"), producto, 201));

        mvc.perform(conAdmin(post("/api/v1/admin/productos"), producto))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El SKU ARE-1 ya existe"));
        var variante = "{\"sku\":\"ARE-1-U\",\"precioAdicional\":0,\"existenciaInicial\":0}";
        enviar(post("/api/v1/admin/productos/" + id + "/variantes"), variante, 201);
        enviar(post("/api/v1/admin/productos/" + id + "/variantes"), variante, 409);
        enviar(post("/api/v1/admin/categorias"), "{\"nombre\":\"Aretes\"}", 409);
    }

    @Test
    void lasCategoriasFormanUnArbolSinCiclos() throws Exception {
        long anillos = id(enviar(post("/api/v1/admin/categorias"), "{\"nombre\":\"Anillos\"}", 201));
        long plata = id(enviar(post("/api/v1/admin/categorias"),
                "{\"nombre\":\"Anillos de plata\",\"idPadre\":%d}".formatted(anillos), 201));

        mvc.perform(get("/api/v1/admin/categorias").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$[0].slug").value("anillos"))
                .andExpect(jsonPath("$[0].hijos[0].slug").value("anillos-de-plata"));
        enviar(put("/api/v1/admin/categorias/" + anillos),
                "{\"nombre\":\"Anillos\",\"idPadre\":%d}".formatted(plata), 409);
        var desactivada = enviar(patch("/api/v1/admin/categorias/" + plata + "/estado"), "{\"activa\":false}", 200);
        assertThat((Boolean) JsonPath.read(desactivada, "$.activa")).isFalse();
    }

    @Test
    void unaImagenQueNoEsImagenSeRechaza() throws Exception {
        long categoria = id(enviar(post("/api/v1/admin/categorias"), "{\"nombre\":\"Pulseras\"}", 201));
        long producto = id(enviar(post("/api/v1/admin/productos"),
                "{\"sku\":\"PUL-1\",\"nombre\":\"Pulsera\",\"idCategoria\":%d,\"precioBase\":100}".formatted(categoria), 201));

        mvc.perform(multipart("/api/v1/admin/productos/" + producto + "/imagenes")
                        .file(new MockMultipartFile("archivo", "foto.png", "image/png", "<script>".getBytes()))
                        .file(new MockMultipartFile("textoAlternativo", "", "text/plain", "Pulsera".getBytes()))
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("JPEG, PNG o WebP")));
    }

    @Test
    void unClienteNoPuedeAdministrarElCatalogo() throws Exception {
        crearCliente("ana@nalu.co");
        mvc.perform(post("/api/v1/admin/categorias").header(HttpHeaders.AUTHORIZATION, bearer("ana@nalu.co"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"X\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void laBaseDeDatosImpideExistenciasNegativas() throws Exception {
        long categoria = id(enviar(post("/api/v1/admin/categorias"), "{\"nombre\":\"Anillos\"}", 201));
        long producto = id(enviar(post("/api/v1/admin/productos"),
                "{\"sku\":\"ANI-2\",\"nombre\":\"Anillo\",\"idCategoria\":%d,\"precioBase\":100}".formatted(categoria), 201));
        enviar(post("/api/v1/admin/productos/" + producto + "/variantes"),
                "{\"sku\":\"ANI-2-6\",\"precioAdicional\":0,\"existenciaInicial\":1}", 201);

        // RNF10: el CHECK de la base de datos es la última barrera aunque se salte la entidad.
        assertThatThrownBy(() -> transaccion.executeWithoutResult(t -> em
                .createQuery("update VarianteProducto v set v.stockActual = -1").executeUpdate()))
                .hasMessageContaining("ck_variante_producto_stock");
        assertThat(variantes.findAll().getFirst().getStockActual()).isEqualTo(1);
    }

    private MockHttpServletRequestBuilder conAdmin(MockHttpServletRequestBuilder solicitud, String cuerpo) {
        return solicitud.header(HttpHeaders.AUTHORIZATION, admin).contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    private String enviar(MockHttpServletRequestBuilder solicitud, String cuerpo, int estadoEsperado) throws Exception {
        return mvc.perform(conAdmin(solicitud, cuerpo)).andExpect(status().is(estadoEsperado))
                .andReturn().getResponse().getContentAsString();
    }

    private static long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
