package co.nalu.negocio.catalogo;

import co.nalu.negocio.contenido.ResenaService;
import co.nalu.datos.entidad.Categoria;
import co.nalu.datos.entidad.EstadoProducto;
import co.nalu.datos.entidad.ImagenProducto;
import co.nalu.datos.entidad.Producto;
import co.nalu.datos.entidad.ProductoMaterial;
import co.nalu.datos.entidad.RolMaterial;
import co.nalu.datos.entidad.VarianteProducto;
import co.nalu.datos.repositorio.CategoriaRepository;
import co.nalu.datos.repositorio.MetadatoSeoRepository;
import co.nalu.datos.repositorio.ProductoRepository;
import co.nalu.negocio.catalogo.dto.CatalogoPublicoDtos.CategoriaDetalle;
import co.nalu.negocio.catalogo.dto.CatalogoPublicoDtos.CategoriaPublica;
import co.nalu.negocio.catalogo.dto.CatalogoPublicoDtos.FichaProducto;
import co.nalu.negocio.catalogo.dto.CatalogoPublicoDtos.FiltroCatalogo;
import co.nalu.negocio.catalogo.dto.CatalogoPublicoDtos.ImagenPublica;
import co.nalu.negocio.catalogo.dto.CatalogoPublicoDtos.MaterialFicha;
import co.nalu.negocio.catalogo.dto.CatalogoPublicoDtos.Miga;
import co.nalu.negocio.catalogo.dto.CatalogoPublicoDtos.ProductoTarjeta;
import co.nalu.negocio.catalogo.dto.CatalogoPublicoDtos.VarianteFicha;
import co.nalu.negocio.comun.Pagina;
import co.nalu.negocio.comun.RecursoNoEncontradoException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tienda pública (RF06, RF07). Solo se ven productos PUBLICADOS de categorías activas; los precios
 * salen de {@link CalculadoraPrecio} y nunca se exponen existencias exactas, solo si hay o no.
 */
@Service
public class CatalogoPublicoService {

    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final MetadatoSeoRepository metadatos;
    private final CalculadoraPrecio calculadora;
    private final ConstructorSeo seo;
    private final ResenaService resenas;

    public CatalogoPublicoService(ProductoRepository productos, CategoriaRepository categorias,
                                  MetadatoSeoRepository metadatos, CalculadoraPrecio calculadora, ConstructorSeo seo,
                                  ResenaService resenas) {
        this.productos = productos;
        this.categorias = categorias;
        this.metadatos = metadatos;
        this.calculadora = calculadora;
        this.seo = seo;
        this.resenas = resenas;
    }

    @Transactional(readOnly = true)
    public List<CategoriaPublica> arbolCategorias() {
        return categorias.findAllByOrderByNombreAsc().stream()
                .filter(c -> c.getPadre() == null && c.isActiva())
                .map(CatalogoPublicoService::categoriaPublica)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaDetalle categoria(String slug) {
        var categoria = categoriaActiva(slug);
        var meta = metadatos.findByCategoriaId(categoria.getId()).orElse(null);
        return new CategoriaDetalle(categoria.getNombre(), categoria.getSlug(), categoria.getDescripcion(),
                migas(categoria), categoria.getHijos().stream().filter(Categoria::isActiva)
                .sorted(Comparator.comparing(Categoria::getNombre)).map(CatalogoPublicoService::categoriaPublica).toList(),
                seo.categoria(categoria, meta));
    }

    @Transactional(readOnly = true)
    public Pagina<ProductoTarjeta> buscar(FiltroCatalogo filtro, Pageable pagina) {
        List<Long> idsCategoria = null;
        if (filtro.categoria() != null && !filtro.categoria().isBlank()) {
            idsCategoria = new ArrayList<>();
            agregarDescendientes(categoriaActiva(filtro.categoria()), idsCategoria);
        }
        Specification<Producto> spec = Specification.allOf(CatalogoPublicoSpecs.visible(),
                CatalogoPublicoSpecs.texto(filtro.q()), CatalogoPublicoSpecs.enCategorias(idsCategoria),
                CatalogoPublicoSpecs.conMaterial(filtro.material()),
                CatalogoPublicoSpecs.precioEntre(filtro.precioMin(), filtro.precioMax()),
                CatalogoPublicoSpecs.disponible(filtro.disponible()));
        return Pagina.de(productos.findAll(spec, pagina), this::tarjeta);
    }

    @Transactional(readOnly = true)
    public FichaProducto ficha(String slug) {
        var producto = productos.findBySlugAndEstado(slug, EstadoProducto.PUBLICADO)
                .filter(p -> p.getCategoria().isActiva())
                .orElseThrow(() -> new RecursoNoEncontradoException("El producto no existe o no está disponible"));
        var variantes = producto.getVariantes().stream()
                .filter(VarianteProducto::isActiva)
                .sorted(Comparator.comparing(VarianteProducto::getId))
                .map(v -> new VarianteFicha(v.getId(), v.getTalla(), calculadora.precioVenta(producto, v), v.disponible() > 0))
                .toList();
        var imagenes = producto.getImagenes().stream()
                .sorted(Comparator.comparing((ImagenProducto i) -> !i.isPrincipal()).thenComparingInt(ImagenProducto::getOrden))
                .map(i -> new ImagenPublica(i.getUrl(), i.getTextoAlternativo()))
                .toList();
        var materiales = producto.getMateriales().stream()
                .sorted(Comparator.comparing(ProductoMaterial::getRol))
                .map(pm -> new MaterialFicha(ConstructorSeo.nombre(pm.getMaterial()), pm.getRol().name(),
                        pm.getPesoGramos(), pm.getQuilates(), pm.getMaterial().isRequiereCertificado()))
                .toList();
        var migas = new ArrayList<>(migas(producto.getCategoria()));
        var meta = metadatos.findByProductoId(producto.getId()).orElse(null);
        var resumenResenas = resenas.publicas(producto.getId());
        return new FichaProducto(producto.getSlug(), producto.getNombre(), producto.getDescripcion(), migas, imagenes,
                materiales, variantes, producto.getPrecioDesde(), variantes.stream().noneMatch(VarianteFicha::disponible),
                resumenResenas, seo.producto(producto, variantes, meta, resumenResenas));
    }

    /** Slugs de lo publicado, para el sitemap. */
    @Transactional(readOnly = true)
    public List<String> slugsPublicados() {
        return productos.findByEstado(EstadoProducto.PUBLICADO).stream()
                .filter(p -> p.getCategoria().isActiva()).map(Producto::getSlug).sorted().toList();
    }

    @Transactional(readOnly = true)
    public List<String> slugsCategorias() {
        return categorias.findAllByOrderByNombreAsc().stream().filter(Categoria::isActiva).map(Categoria::getSlug).toList();
    }

    private ProductoTarjeta tarjeta(Producto p) {
        var imagen = p.getImagenes().stream().filter(ImagenProducto::isPrincipal).findFirst()
                .map(i -> new ImagenPublica(i.getUrl(), i.getTextoAlternativo())).orElse(null);
        var material = p.getMateriales().stream()
                .min(Comparator.comparing((ProductoMaterial pm) -> pm.getRol() != RolMaterial.METAL_PRINCIPAL))
                .map(pm -> ConstructorSeo.nombre(pm.getMaterial())).orElse(null);
        boolean agotado = p.getVariantes().stream().filter(VarianteProducto::isActiva).noneMatch(v -> v.disponible() > 0);
        return new ProductoTarjeta(p.getSlug(), p.getNombre(), p.getCategoria().getNombre(), imagen, material,
                p.getPrecioDesde(), agotado);
    }

    private Categoria categoriaActiva(String slug) {
        return categorias.findBySlugAndActivaTrue(slug)
                .orElseThrow(() -> new RecursoNoEncontradoException("La categoría no existe"));
    }

    private static void agregarDescendientes(Categoria categoria, List<Long> ids) {
        ids.add(categoria.getId());
        categoria.getHijos().stream().filter(Categoria::isActiva).forEach(h -> agregarDescendientes(h, ids));
    }

    private static List<Miga> migas(Categoria categoria) {
        var migas = new ArrayList<Miga>();
        for (var c = categoria; c != null; c = c.getPadre()) {
            migas.addFirst(new Miga(c.getNombre(), c.getSlug()));
        }
        return migas;
    }

    private static CategoriaPublica categoriaPublica(Categoria c) {
        return new CategoriaPublica(c.getNombre(), c.getSlug(), c.getDescripcion(), c.getHijos().stream()
                .filter(Categoria::isActiva).sorted(Comparator.comparing(Categoria::getNombre))
                .map(CatalogoPublicoService::categoriaPublica).toList());
    }
}
