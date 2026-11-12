package co.nalu.datos.entidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de las reglas de publicación y de las colecciones de {@link Producto}, sin
 * Spring ni base de datos.
 */
class ProductoTest {

    private final Categoria categoria = new Categoria("Anillos", "anillos", null, null);

    private Producto producto() {
        return new Producto("AN-001", "Anillo Solitario", "anillo-solitario", null, categoria,
                new BigDecimal("189000.00"));
    }

    private ProductoMaterial material(Producto producto, Material material, RolMaterial rol) {
        return new ProductoMaterial(producto, material, rol, null, null);
    }

    private VarianteProducto variante(Producto producto) {
        return new VarianteProducto(producto, "AN-001-16", "16", null, BigDecimal.ZERO, 3);
    }

    private ImagenProducto imagen(Producto producto, String url, int orden) {
        return new ImagenProducto(producto, url, "Anillo de plata visto de frente", orden);
    }

    private Producto productoCompleto() {
        Producto producto = producto();
        var plata = new Material("Plata", "925", true, new BigDecimal("1.000"));
        producto.reemplazarMateriales(List.of(
                material(producto, plata, RolMaterial.METAL_PRINCIPAL)));
        producto.agregarVariante(variante(producto));
        producto.agregarImagen(imagen(producto, "/archivos/productos/an-001.jpg", 1));
        return producto;
    }

    @Test
    void publicarFallaSiLaCategoriaEstaInactiva() {
        Producto producto = productoCompleto();
        categoria.desactivar();

        assertThat(producto.requisitosPendientesParaPublicar())
                .containsExactly("La categoría del producto debe estar activa");
        assertThatThrownBy(producto::publicar)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("categoría");
        assertThat(producto.getEstado()).isEqualTo(EstadoProducto.BORRADOR);
    }

    @Test
    void publicarFallaSiNoTieneMateriales() {
        Producto producto = producto();
        producto.agregarVariante(variante(producto));
        producto.agregarImagen(imagen(producto, "/archivos/productos/an-001.jpg", 1));

        assertThat(producto.requisitosPendientesParaPublicar())
                .containsExactly("El producto debe tener al menos un material");
    }

    @Test
    void publicarFallaSiNoTieneVariantesActivas() {
        Producto producto = producto();
        var plata = new Material("Plata", "925", true, new BigDecimal("1.000"));
        producto.reemplazarMateriales(List.of(
                material(producto, plata, RolMaterial.METAL_PRINCIPAL)));
        VarianteProducto variante = variante(producto);
        variante.actualizar("16", null, BigDecimal.ZERO, false);
        producto.agregarVariante(variante);
        producto.agregarImagen(imagen(producto, "/archivos/productos/an-001.jpg", 1));

        assertThat(producto.requisitosPendientesParaPublicar())
                .containsExactly("El producto debe tener al menos una variante activa");
    }

    @Test
    void publicarFallaSiNoTieneImagenes() {
        Producto producto = producto();
        var plata = new Material("Plata", "925", true, new BigDecimal("1.000"));
        producto.reemplazarMateriales(List.of(
                material(producto, plata, RolMaterial.METAL_PRINCIPAL)));
        producto.agregarVariante(variante(producto));

        assertThat(producto.requisitosPendientesParaPublicar())
                .containsExactly("El producto debe tener al menos una imagen");
    }

    @Test
    void publicarConTodosLosRequisitosCambiaElEstado() {
        Producto producto = productoCompleto();

        assertThat(producto.requisitosPendientesParaPublicar()).isEmpty();
        producto.publicar();

        assertThat(producto.getEstado()).isEqualTo(EstadoProducto.PUBLICADO);
    }

    @Test
    void laPrimeraImagenQuedaComoPrincipal() {
        Producto producto = producto();
        ImagenProducto primera = imagen(producto, "/archivos/productos/1.jpg", 1);
        ImagenProducto segunda = imagen(producto, "/archivos/productos/2.jpg", 2);

        producto.agregarImagen(primera);
        producto.agregarImagen(segunda);

        assertThat(primera.isPrincipal()).isTrue();
        assertThat(segunda.isPrincipal()).isFalse();
    }

    @Test
    void marcarOtraImagenComoPrincipalDesmarcaLaAnterior() {
        Producto producto = producto();
        ImagenProducto primera = imagen(producto, "/archivos/productos/1.jpg", 1);
        ImagenProducto segunda = imagen(producto, "/archivos/productos/2.jpg", 2);
        producto.agregarImagen(primera);
        producto.agregarImagen(segunda);

        producto.marcarImagenPrincipal(segunda);

        assertThat(primera.isPrincipal()).isFalse();
        assertThat(segunda.isPrincipal()).isTrue();
    }

    @Test
    void quitarLaImagenPrincipalReasignaLaDeMenorOrden() {
        Producto producto = producto();
        ImagenProducto principal = imagen(producto, "/archivos/productos/1.jpg", 2);
        ImagenProducto menorOrden = imagen(producto, "/archivos/productos/2.jpg", 1);
        ImagenProducto ultima = imagen(producto, "/archivos/productos/3.jpg", 3);
        producto.agregarImagen(principal);
        producto.agregarImagen(menorOrden);
        producto.agregarImagen(ultima);

        producto.quitarImagen(principal);

        assertThat(producto.getImagenes()).containsExactly(menorOrden, ultima);
        assertThat(menorOrden.isPrincipal()).isTrue();
        assertThat(ultima.isPrincipal()).isFalse();
    }

    @Test
    void reemplazarMaterialesConUnMaterialRepetidoFalla() {
        Producto producto = producto();
        var plata = new Material("Plata", "925", true, new BigDecimal("1.000"));
        var cuarzo = new Material("Cuarzo", null, false, new BigDecimal("1.000"));

        assertThatThrownBy(() -> producto.reemplazarMateriales(List.of(
                material(producto, plata, RolMaterial.METAL_PRINCIPAL),
                material(producto, plata, RolMaterial.SECUNDARIO))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plata");

        producto.reemplazarMateriales(List.of(
                material(producto, plata, RolMaterial.METAL_PRINCIPAL),
                material(producto, cuarzo, RolMaterial.PIEDRA)));
        assertThat(producto.getMateriales()).hasSize(2);
    }
}
