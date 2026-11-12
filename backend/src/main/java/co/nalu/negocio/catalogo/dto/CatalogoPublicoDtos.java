package co.nalu.negocio.catalogo.dto;

import co.nalu.negocio.contenido.dto.ResenaDtos.Resumen;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Respuestas públicas de la tienda (RF06, RF07). Contrato: docs/arquitectura/api-fase2.md. */
public final class CatalogoPublicoDtos {

    private CatalogoPublicoDtos() {}

    public record ImagenPublica(String url, String textoAlternativo) {}

    public record ProductoTarjeta(String slug, String nombre, String categoria, ImagenPublica imagen,
                                  String material, BigDecimal precioDesde, boolean agotado) {}

    public record CategoriaPublica(String nombre, String slug, String descripcion, List<CategoriaPublica> hijos) {}

    public record Miga(String nombre, String slug) {}

    public record Seo(String titulo, String descripcion, String palabrasClave, String urlCanonica,
                      Map<String, Object> jsonLd) {}

    public record CategoriaDetalle(String nombre, String slug, String descripcion, List<Miga> migas,
                                   List<CategoriaPublica> hijos, Seo seo) {}

    public record MaterialFicha(String nombre, String rol, BigDecimal pesoGramos, BigDecimal quilates,
                                boolean requiereCertificado) {}

    public record VarianteFicha(Long id, String talla, BigDecimal precio, boolean disponible) {}

    public record FichaProducto(String slug, String nombre, String descripcion, List<Miga> migas,
                                List<ImagenPublica> imagenes, List<MaterialFicha> materiales,
                                List<VarianteFicha> variantes, BigDecimal precioDesde, boolean agotado,
                                Resumen resenas, Seo seo) {}

    public record FiltroCatalogo(String q, String categoria, Long material, BigDecimal precioMin, BigDecimal precioMax,
                                 Boolean disponible) {}
}
