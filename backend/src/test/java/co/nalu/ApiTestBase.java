package co.nalu;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.nalu.datos.entidad.Administrador;
import co.nalu.datos.entidad.Cliente;
import co.nalu.datos.repositorio.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Base de las pruebas de API contra PostgreSQL real. Todas comparten el mismo contexto de Spring
 * (y el mismo contenedor); cada prueba empieza con la base de datos vacía.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public abstract class ApiTestBase {

    protected static final String CONTRASENA = "contrasena-segura-123";

    @Autowired protected MockMvc mvc;
    @Autowired protected UsuarioRepository usuarios;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired private EntityManager em;
    @Autowired private TransactionTemplate transaccion;

    @BeforeEach
    void vaciarBaseDeDatos() {
        transaccion.executeWithoutResult(t -> em.createNativeQuery("""
                TRUNCATE auditoria, resena, envio, evento_pasarela, pago, historial_estado_pedido, detalle_pedido, pedido, item_carrito, carrito,
                         promocion, movimiento_inventario, imagen_producto, producto_material, variante_producto, producto,
                         metadato_seo, categoria, material, direccion, refresh_token, intento_login, cliente, administrador, usuario
                RESTART IDENTITY CASCADE""").executeUpdate());
    }

    protected Cliente crearCliente(String correo) {
        return usuarios.save(new Cliente("Ana", "Pérez", correo, "3001234567", passwordEncoder.encode(CONTRASENA), null,
                false, OffsetDateTime.now()));
    }

    protected Administrador crearAdministrador(String correo) {
        return usuarios.save(new Administrador("Luis", "Gómez", correo, passwordEncoder.encode(CONTRASENA)));
    }

    /** PNG válido de 1×1 píxel. */
    protected static final byte[] PNG = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

    /**
     * Crea por la API (como administrador) una categoría, un material de plata con factor 1 y un
     * producto publicado con una variante por cada existencia indicada. Devuelve los ids de las
     * variantes, en orden.
     */
    protected java.util.List<Long> productoPublicado(String admin, String sku, String precioBase, int... existencias)
            throws Exception {
        long categoria = idDe(json(admin, post("/api/v1/admin/categorias"), "{\"nombre\":\"Cat %s\"}".formatted(sku)));
        long material = idDe(json(admin, post("/api/v1/admin/materiales"),
                "{\"tipo\":\"Plata %s\",\"ley\":\"925\",\"factorPrecio\":1}".formatted(sku)));
        long producto = idDe(json(admin, post("/api/v1/admin/productos"),
                "{\"sku\":\"%s\",\"nombre\":\"Pieza %s\",\"idCategoria\":%d,\"precioBase\":%s}"
                        .formatted(sku, sku, categoria, precioBase)));
        json(admin, org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                "/api/v1/admin/productos/" + producto + "/materiales"),
                "[{\"idMaterial\":%d,\"rol\":\"METAL_PRINCIPAL\"}]".formatted(material));
        String ultimo = null;
        for (int i = 0; i < existencias.length; i++) {
            ultimo = json(admin, post("/api/v1/admin/productos/" + producto + "/variantes"),
                    "{\"sku\":\"%s-%d\",\"talla\":\"%d\",\"precioAdicional\":0,\"existenciaInicial\":%d}"
                            .formatted(sku, i, i + 6, existencias[i]));
        }
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/v1/admin/productos/" + producto + "/imagenes")
                        .file(new org.springframework.mock.web.MockMultipartFile("archivo", "a.png", "image/png", PNG))
                        .file(new org.springframework.mock.web.MockMultipartFile("textoAlternativo", "", "text/plain",
                                "Pieza".getBytes()))
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isCreated());
        json(admin, org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/v1/admin/productos/" + producto + "/estado"), "{\"estado\":\"PUBLICADO\"}");
        java.util.List<Number> ids = JsonPath.read(ultimo, "$.variantes[*].id");
        return ids.stream().map(Number::longValue).toList();
    }

    /** Envía JSON con el token dado, exige 2xx y devuelve el cuerpo. */
    protected String json(String bearer, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder solicitud,
                          String cuerpo) throws Exception {
        return mvc.perform(solicitud.header(org.springframework.http.HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
    }

    protected static long idDe(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    /** Inicia sesión por la API y devuelve el header Authorization listo para usar. */
    protected String bearer(String correo) throws Exception {
        var respuesta = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, CONTRASENA)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(respuesta, "$.accessToken");
    }
}
