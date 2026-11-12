package co.nalu.api.publico;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.nalu.ApiTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** RF05, RF06 y RF07: catálogo público, ficha, precio de venta (ADR 003) y metadatos SEO. */
class CatalogoPublicoTest extends ApiTestBase {

    private static final byte[] PNG = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

    private String admin;
    private long anillos;
    private long anillosPlata;
    private long plata;
    private long cuarzo;

    @BeforeEach
    void catalogo() throws Exception {
        crearAdministrador("admin@nalu.co");
        admin = bearer("admin@nalu.co");
        anillos = id(enviar(post("/api/v1/admin/categorias"), "{\"nombre\":\"Anillos\"}"));
        anillosPlata = id(enviar(post("/api/v1/admin/categorias"),
                "{\"nombre\":\"Anillos de plata\",\"idPadre\":%d}".formatted(anillos)));
        plata = id(enviar(post("/api/v1/admin/materiales"), "{\"tipo\":\"Plata\",\"ley\":\"925\",\"factorPrecio\":1.5}"));
        cuarzo = id(enviar(post("/api/v1/admin/materiales"), "{\"tipo\":\"Cuarzo\",\"factorPrecio\":1}"));
    }

    @Test
    void elPrecioDeVentaAplicaElFactorDelMetalPrincipal() throws Exception {
        publicar("ANI-LUNA", "Anillo Luna", anillosPlata, "100000", new int[] {0, 20000}, new int[] {3, 0});

        mvc.perform(get("/api/v1/productos/anillo-luna"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variantes[0].precio").value(150000))
                .andExpect(jsonPath("$.variantes[1].precio").value(170000))
                .andExpect(jsonPath("$.variantes[1].disponible").value(false))
                .andExpect(jsonPath("$.precioDesde").value(150000))
                .andExpect(jsonPath("$.agotado").value(false))
                .andExpect(jsonPath("$.migas[*].slug").value(contains("anillos", "anillos-de-plata")))
                .andExpect(jsonPath("$.materiales[0].nombre").value("Plata 925"))
                .andExpect(jsonPath("$.seo.jsonLd['@type']").value("Product"))
                .andExpect(jsonPath("$.seo.jsonLd.offers['@type']").value("AggregateOffer"))
                .andExpect(jsonPath("$.seo.jsonLd.offers.lowPrice").value("150000"))
                .andExpect(jsonPath("$.seo.jsonLd.offers.priceCurrency").value("COP"))
                .andExpect(jsonPath("$.seo.jsonLd.offers.availability").value("https://schema.org/InStock"))
                .andExpect(jsonPath("$.seo.urlCanonica").value("http://localhost:4200/producto/anillo-luna"));

        // Cambiar el factor del metal recalcula el precio de todas sus piezas.
        enviar(put("/api/v1/admin/materiales/" + plata), "{\"tipo\":\"Plata\",\"ley\":\"925\",\"factorPrecio\":2}");
        mvc.perform(get("/api/v1/productos").param("q", "luna"))
                .andExpect(jsonPath("$.contenido[0].precioDesde").value(200000));
    }

    @Test
    void soloSeVeLoPublicadoDeCategoriasActivas() throws Exception {
        publicar("ANI-1", "Anillo Sol", anillosPlata, "100000", new int[] {0}, new int[] {1});
        id(enviar(post("/api/v1/admin/productos"),
                "{\"sku\":\"ANI-2\",\"nombre\":\"Anillo Borrador\",\"idCategoria\":%d,\"precioBase\":1}".formatted(anillos)));

        mvc.perform(get("/api/v1/productos")).andExpect(jsonPath("$.totalElementos").value(1));
        mvc.perform(get("/api/v1/productos/anillo-borrador")).andExpect(status().isNotFound());

        enviar(patch("/api/v1/admin/categorias/" + anillosPlata + "/estado"), "{\"activa\":false}");
        mvc.perform(get("/api/v1/productos")).andExpect(jsonPath("$.totalElementos").value(0));
        mvc.perform(get("/api/v1/productos/anillo-sol")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/categorias")).andExpect(jsonPath("$[0].hijos.length()").value(0));
    }

    @Test
    void filtrosPorCategoriaMaterialPrecioYDisponibilidad() throws Exception {
        publicar("ANI-A", "Anillo Agua", anillosPlata, "100000", new int[] {0}, new int[] {2});      // 150.000
        publicar("ANI-B", "Anillo Brisa", anillos, "300000", new int[] {0}, new int[] {0});          // 450.000, agotado
        long collares = id(enviar(post("/api/v1/admin/categorias"), "{\"nombre\":\"Collares\"}"));
        publicar("COL-A", "Collar Alba", collares, "80000", new int[] {0}, new int[] {5});          // 120.000

        // La categoría padre incluye sus subcategorías.
        mvc.perform(get("/api/v1/productos").param("categoria", "anillos"))
                .andExpect(jsonPath("$.contenido[*].slug").value(containsInAnyOrder("anillo-agua", "anillo-brisa")));
        mvc.perform(get("/api/v1/productos").param("material", String.valueOf(plata)))
                .andExpect(jsonPath("$.totalElementos").value(3));
        mvc.perform(get("/api/v1/productos").param("precioMin", "130000").param("precioMax", "500000"))
                .andExpect(jsonPath("$.contenido[*].slug").value(containsInAnyOrder("anillo-agua", "anillo-brisa")));
        mvc.perform(get("/api/v1/productos").param("disponible", "true"))
                .andExpect(jsonPath("$.contenido[*].slug").value(containsInAnyOrder("anillo-agua", "collar-alba")));
        mvc.perform(get("/api/v1/productos").param("sort", "precioDesde,asc"))
                .andExpect(jsonPath("$.contenido[*].slug").value(contains("collar-alba", "anillo-agua", "anillo-brisa")));
        mvc.perform(get("/api/v1/productos").param("q", "brisa"))
                .andExpect(jsonPath("$.contenido[0].agotado").value(true))
                .andExpect(jsonPath("$.contenido[0].material").value("Plata 925"));
        mvc.perform(get("/api/v1/productos").param("categoria", "no-existe")).andExpect(status().isNotFound());
    }

    @Test
    void elAdministradorEditaElSeoYLaFichaLoUsa() throws Exception {
        long producto = publicar("ANI-S", "Anillo Seo", anillos, "100000", new int[] {0}, new int[] {1});

        enviar(put("/api/v1/admin/seo/productos/" + producto), """
                {"titulo":"Anillo de plata 925 hecho a mano | Nalu","metaDescripcion":"Anillo artesanal.",
                 "palabrasClave":"anillo, plata","datosEstructurados":{"brand":{"@type":"Brand","name":"Nalu"}}}""");
        mvc.perform(get("/api/v1/productos/anillo-seo"))
                .andExpect(jsonPath("$.seo.titulo").value("Anillo de plata 925 hecho a mano | Nalu"))
                .andExpect(jsonPath("$.seo.descripcion").value("Anillo artesanal."))
                .andExpect(jsonPath("$.seo.jsonLd.brand.name").value("Nalu"))
                .andExpect(jsonPath("$.seo.jsonLd.offers['@type']").value("Offer"));

        mvc.perform(conAdmin(put("/api/v1/admin/seo/productos/" + producto), "{\"urlCanonica\":\"no es url\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/seo/rutas")).andExpect(jsonPath("$.productos[0]").value("anillo-seo"));
    }

    @Test
    void laCategoriaPublicaTraeSuSeo() throws Exception {
        mvc.perform(get("/api/v1/categorias/anillos-de-plata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.migas[*].slug").value(contains("anillos", "anillos-de-plata")))
                .andExpect(jsonPath("$.seo.titulo").value("Anillos de plata · Joyería Nalu"))
                .andExpect(jsonPath("$.seo.jsonLd['@type']").value("CollectionPage"));
    }

    /** Crea un producto con Plata (metal principal), una variante por precio adicional, una imagen y lo publica. */
    private long publicar(String sku, String nombre, long categoria, String precioBase, int[] adicionales,
                          int[] existencias) throws Exception {
        long id = id(enviar(post("/api/v1/admin/productos"),
                "{\"sku\":\"%s\",\"nombre\":\"%s\",\"descripcion\":\"%s en plata.\",\"idCategoria\":%d,\"precioBase\":%s}"
                        .formatted(sku, nombre, nombre, categoria, precioBase)));
        enviar(put("/api/v1/admin/productos/" + id + "/materiales"), """
                [{"idMaterial":%d,"rol":"METAL_PRINCIPAL"},{"idMaterial":%d,"rol":"PIEDRA"}]""".formatted(plata, cuarzo));
        for (int i = 0; i < adicionales.length; i++) {
            enviar(post("/api/v1/admin/productos/" + id + "/variantes"),
                    "{\"sku\":\"%s-%d\",\"talla\":\"%d\",\"precioAdicional\":%d,\"existenciaInicial\":%d}"
                            .formatted(sku, i, 6 + i, adicionales[i], existencias[i]));
        }
        mvc.perform(multipart("/api/v1/admin/productos/" + id + "/imagenes")
                        .file(new MockMultipartFile("archivo", "a.png", "image/png", PNG))
                        .file(new MockMultipartFile("textoAlternativo", "", "text/plain", nombre.getBytes()))
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isCreated());
        enviar(patch("/api/v1/admin/productos/" + id + "/estado"), "{\"estado\":\"PUBLICADO\"}");
        return id;
    }

    private MockHttpServletRequestBuilder conAdmin(MockHttpServletRequestBuilder solicitud, String cuerpo) {
        return solicitud.header(HttpHeaders.AUTHORIZATION, admin).contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    private String enviar(MockHttpServletRequestBuilder solicitud, String cuerpo) throws Exception {
        return mvc.perform(conAdmin(solicitud, cuerpo)).andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
    }

    private static long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
