package co.nalu.negocio.catalogo;

import co.nalu.negocio.contenido.dto.ResenaDtos.Resumen;
import co.nalu.config.AppProperties;
import co.nalu.datos.entidad.Categoria;
import co.nalu.datos.entidad.ImagenProducto;
import co.nalu.datos.entidad.Material;
import co.nalu.datos.entidad.MetadatoSeo;
import co.nalu.datos.entidad.Producto;
import co.nalu.negocio.catalogo.dto.CatalogoPublicoDtos.Seo;
import co.nalu.negocio.catalogo.dto.CatalogoPublicoDtos.VarianteFicha;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Metadatos de las páginas públicas (RF05, RNF07): título, descripción, URL canónica y JSON-LD
 * Schema.org {@code Product} + {@code Offer}/{@code AggregateOffer} generado con los datos reales
 * (precio y disponibilidad siempre actuales). Lo que el administrador define en
 * {@link MetadatoSeo} tiene prioridad.
 */
@Component
class ConstructorSeo {

    static final int MAXIMO_DESCRIPCION = 160;

    private final AppProperties app;

    ConstructorSeo(AppProperties app) {
        this.app = app;
    }

    Seo producto(Producto p, List<VarianteFicha> variantes, MetadatoSeo meta,
                 Resumen resenas) {
        var url = app.origenFrontend() + "/producto/" + p.getSlug();
        var titulo = valor(meta == null ? null : meta.getTitulo(), p.getNombre() + " · Nalu");
        var descripcion = valor(meta == null ? null : meta.getMetaDescripcion(), resumen(p.getDescripcion(), p.getNombre()));
        var canonica = valor(meta == null ? null : meta.getUrlCanonica(), url);

        var jsonLd = new LinkedHashMap<String, Object>();
        jsonLd.put("@context", "https://schema.org");
        jsonLd.put("@type", "Product");
        jsonLd.put("name", p.getNombre());
        jsonLd.put("description", descripcion);
        jsonLd.put("sku", p.getSku());
        jsonLd.put("url", canonica);
        jsonLd.put("category", p.getCategoria().getNombre());
        var imagenes = p.getImagenes().stream().sorted(Comparator.comparing(i -> !i.isPrincipal()))
                .map(ImagenProducto::getUrl).map(u -> app.origenFrontend() + u).toList();
        if (!imagenes.isEmpty()) {
            jsonLd.put("image", imagenes);
        }
        var materiales = p.getMateriales().stream().map(pm -> nombre(pm.getMaterial())).toList();
        if (!materiales.isEmpty()) {
            jsonLd.put("material", String.join(", ", materiales));
        }
        jsonLd.put("offers", oferta(variantes, canonica));
        if (resenas != null && resenas.total() > 0) {
            var valoracion = new LinkedHashMap<String, Object>();
            valoracion.put("@type", "AggregateRating");
            valoracion.put("ratingValue", resenas.promedio());
            valoracion.put("reviewCount", resenas.total());
            jsonLd.put("aggregateRating", valoracion);
        }
        if (meta != null && meta.getDatosEstructurados() != null) {
            meta.getDatosEstructurados().forEach((clave, v) -> {
                if (!clave.equals("@context") && !clave.equals("@type") && !clave.equals("offers")) {
                    jsonLd.put(clave, v);
                }
            });
        }
        return new Seo(titulo, descripcion, meta == null ? null : meta.getPalabrasClave(), canonica, jsonLd);
    }

    Seo categoria(Categoria c, MetadatoSeo meta) {
        var url = app.origenFrontend() + "/catalogo/" + c.getSlug();
        var titulo = valor(meta == null ? null : meta.getTitulo(), c.getNombre() + " · Joyería Nalu");
        var descripcion = valor(meta == null ? null : meta.getMetaDescripcion(),
                resumen(c.getDescripcion(), c.getNombre() + " de Nalu, joyería colombiana."));
        var canonica = valor(meta == null ? null : meta.getUrlCanonica(), url);
        var jsonLd = new LinkedHashMap<String, Object>();
        jsonLd.put("@context", "https://schema.org");
        jsonLd.put("@type", "CollectionPage");
        jsonLd.put("name", c.getNombre());
        jsonLd.put("url", canonica);
        if (meta != null && meta.getDatosEstructurados() != null) {
            meta.getDatosEstructurados().forEach(jsonLd::putIfAbsent);
        }
        return new Seo(titulo, descripcion, meta == null ? null : meta.getPalabrasClave(), canonica, jsonLd);
    }

    private static Map<String, Object> oferta(List<VarianteFicha> variantes, String url) {
        var oferta = new LinkedHashMap<String, Object>();
        var precios = variantes.stream().map(VarianteFicha::precio).sorted().toList();
        boolean disponible = variantes.stream().anyMatch(VarianteFicha::disponible);
        BigDecimal minimo = precios.isEmpty() ? BigDecimal.ZERO : precios.getFirst();
        BigDecimal maximo = precios.isEmpty() ? BigDecimal.ZERO : precios.getLast();
        if (minimo.compareTo(maximo) == 0) {
            oferta.put("@type", "Offer");
            oferta.put("price", minimo.stripTrailingZeros().toPlainString());
        } else {
            oferta.put("@type", "AggregateOffer");
            oferta.put("lowPrice", minimo.stripTrailingZeros().toPlainString());
            oferta.put("highPrice", maximo.stripTrailingZeros().toPlainString());
            oferta.put("offerCount", precios.size());
        }
        oferta.put("priceCurrency", "COP");
        oferta.put("availability", disponible ? "https://schema.org/InStock" : "https://schema.org/OutOfStock");
        oferta.put("url", url);
        return oferta;
    }

    static String nombre(Material m) {
        return m.getLey() == null ? m.getTipo() : m.getTipo() + " " + m.getLey();
    }

    private static String valor(String preferido, String porDefecto) {
        return preferido == null || preferido.isBlank() ? porDefecto : preferido;
    }

    /** Primera parte del texto, cortada en un límite de palabra, apta para meta description. */
    static String resumen(String texto, String alternativa) {
        if (texto == null || texto.isBlank()) {
            return alternativa;
        }
        var limpio = texto.strip().replaceAll("\\s+", " ");
        if (limpio.length() <= MAXIMO_DESCRIPCION) {
            return limpio;
        }
        var corte = limpio.lastIndexOf(' ', MAXIMO_DESCRIPCION - 1);
        return limpio.substring(0, corte > 80 ? corte : MAXIMO_DESCRIPCION - 1) + "…";
    }
}
