package co.nalu.negocio.catalogo;

import co.nalu.datos.archivo.AlmacenamientoArchivos;
import co.nalu.datos.entidad.Administrador;
import co.nalu.datos.entidad.EstadoProducto;
import co.nalu.datos.entidad.ImagenProducto;
import co.nalu.datos.entidad.MovimientoInventario;
import co.nalu.datos.entidad.Producto;
import co.nalu.datos.entidad.ProductoMaterial;
import co.nalu.datos.entidad.TipoMovimiento;
import co.nalu.datos.entidad.VarianteProducto;
import co.nalu.datos.repositorio.MovimientoInventarioRepository;
import co.nalu.datos.repositorio.ProductoRepository;
import co.nalu.datos.repositorio.UsuarioRepository;
import co.nalu.datos.repositorio.VarianteProductoRepository;
import co.nalu.negocio.auditoria.AuditoriaService;
import co.nalu.negocio.catalogo.dto.EstadoProductoRequest;
import co.nalu.negocio.catalogo.dto.FiltroProductos;
import co.nalu.negocio.catalogo.dto.ImagenRequest;
import co.nalu.negocio.catalogo.dto.MaterialProductoRequest;
import co.nalu.negocio.catalogo.dto.ProductoAdminDto;
import co.nalu.negocio.catalogo.dto.ProductoRequest;
import co.nalu.negocio.catalogo.dto.ProductoResumenDto;
import co.nalu.negocio.catalogo.dto.VarianteNuevaRequest;
import co.nalu.negocio.catalogo.dto.VarianteRequest;
import co.nalu.negocio.comun.ConflictoException;
import co.nalu.negocio.comun.Pagina;
import co.nalu.negocio.comun.RecursoNoEncontradoException;
import co.nalu.negocio.comun.Slugs;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** RF04 y CU-11: productos con sus materiales, variantes, imágenes y estado de publicación. */
@Service
public class ProductoAdminService {

    static final int TAMANO_MAXIMO_IMAGEN = 5 * 1024 * 1024;
    static final String MOTIVO_EXISTENCIA_INICIAL = "Existencia inicial";

    private final ProductoRepository productos;
    private final VarianteProductoRepository variantes;
    private final MovimientoInventarioRepository movimientos;
    private final UsuarioRepository usuarios;
    private final CategoriaService categorias;
    private final MaterialService materiales;
    private final AlmacenamientoArchivos archivos;
    private final CalculadoraPrecio calculadora;
    private final AuditoriaService auditoria;

    public ProductoAdminService(ProductoRepository productos, VarianteProductoRepository variantes,
                                MovimientoInventarioRepository movimientos, UsuarioRepository usuarios,
                                CategoriaService categorias, MaterialService materiales,
                                AlmacenamientoArchivos archivos, CalculadoraPrecio calculadora,
                                AuditoriaService auditoria) {
        this.productos = productos;
        this.variantes = variantes;
        this.movimientos = movimientos;
        this.usuarios = usuarios;
        this.categorias = categorias;
        this.materiales = materiales;
        this.archivos = archivos;
        this.calculadora = calculadora;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public Pagina<ProductoResumenDto> listar(FiltroProductos criterios, Pageable pagina) {
        Specification<Producto> filtro = Specification.allOf(ProductoSpecs.textoEnNombreOSku(criterios.q()),
                ProductoSpecs.conEstado(criterios.estado()), ProductoSpecs.deCategoria(criterios.idCategoria()));
        return Pagina.de(productos.findAll(filtro, pagina), CatalogoMapper::resumen);
    }

    @Transactional(readOnly = true)
    public ProductoAdminDto obtener(long id) {
        return CatalogoMapper.producto(buscar(id));
    }

    /** Recalcula el precio derivado tras cualquier cambio y devuelve el DTO. */
    private ProductoAdminDto guardado(Producto producto) {
        producto.actualizarPrecioDesde(calculadora.precioDesde(producto));
        return CatalogoMapper.producto(producto);
    }

    @Transactional
    public ProductoAdminDto crear(ProductoRequest solicitud) {
        var slug = Slugs.elegir(solicitud.slug(), solicitud.nombre());
        if (productos.existsBySku(solicitud.sku())) {
            throw skuRepetido(solicitud.sku());
        }
        if (productos.existsBySlug(slug)) {
            throw new ConflictoException("Ya existe un producto con el slug " + slug);
        }
        var producto = new Producto(solicitud.sku(), solicitud.nombre(), slug, solicitud.descripcion(),
                categorias.buscar(solicitud.idCategoria()), solicitud.precioBase());
        return guardado(productos.save(producto));
    }

    @Transactional
    public ProductoAdminDto actualizar(long id, ProductoRequest solicitud) {
        var producto = buscar(id);
        var slug = Slugs.elegir(solicitud.slug(), solicitud.nombre());
        if (productos.existsBySkuAndIdNot(solicitud.sku(), id)) {
            throw skuRepetido(solicitud.sku());
        }
        if (productos.existsBySlugAndIdNot(slug, id)) {
            throw new ConflictoException("Ya existe un producto con el slug " + slug);
        }
        if (producto.getPrecioBase().compareTo(solicitud.precioBase()) != 0) {
            auditoria.registrarCambio("CAMBIO_PRECIO", "productos", id,
                    "Precio base " + producto.getPrecioBase().toPlainString() + " → " + solicitud.precioBase().toPlainString());
        }
        producto.actualizar(solicitud.sku(), solicitud.nombre(), slug, solicitud.descripcion(),
                categorias.buscar(solicitud.idCategoria()), solicitud.precioBase());
        return guardado(producto);
    }

    @Transactional
    public ProductoAdminDto reemplazarMateriales(long id, List<MaterialProductoRequest> solicitud) {
        var producto = buscar(id);
        var nuevos = solicitud.stream()
                .map(m -> new ProductoMaterial(producto, materiales.buscar(m.idMaterial()), m.rol(), m.pesoGramos(),
                        m.quilates()))
                .toList();
        producto.reemplazarMateriales(nuevos);
        return guardado(producto);
    }

    /** CU-11 paso 3; la existencia inicial queda como movimiento de inventario (RF13, RNF15). */
    @Transactional
    public ProductoAdminDto agregarVariante(long id, VarianteNuevaRequest solicitud, long idAdministrador) {
        var producto = buscar(id);
        if (variantes.existsBySku(solicitud.sku())) {
            throw skuRepetido(solicitud.sku());
        }
        var variante = new VarianteProducto(producto, solicitud.sku(), solicitud.talla(), solicitud.pesoGramos(),
                solicitud.precioAdicional(), solicitud.existenciaInicial());
        producto.agregarVariante(variante);
        variantes.save(variante);
        if (solicitud.existenciaInicial() > 0) {
            movimientos.save(new MovimientoInventario(variante, administrador(idAdministrador), TipoMovimiento.ENTRADA,
                    solicitud.existenciaInicial(), MOTIVO_EXISTENCIA_INICIAL));
        }
        return guardado(producto);
    }

    @Transactional
    public ProductoAdminDto actualizarVariante(long id, long idVariante, VarianteRequest solicitud) {
        var producto = buscar(id);
        var variante = producto.getVariantes().stream().filter(v -> v.getId().equals(idVariante)).findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("La variante no existe"));
        if (variante.getPrecioAdicional().compareTo(solicitud.precioAdicional()) != 0) {
            auditoria.registrarCambio("CAMBIO_PRECIO", "productos", id, "Precio adicional de " + variante.getSku() + " "
                    + variante.getPrecioAdicional().toPlainString() + " → " + solicitud.precioAdicional().toPlainString());
        }
        variante.actualizar(solicitud.talla(), solicitud.pesoGramos(), solicitud.precioAdicional(), solicitud.activa());
        if (producto.getEstado() == EstadoProducto.PUBLICADO && !producto.requisitosPendientesParaPublicar().isEmpty()) {
            throw new ConflictoException("No se puede desactivar la única variante activa de un producto publicado. "
                    + "Pásalo antes a borrador.");
        }
        return guardado(producto);
    }

    @Transactional
    public ProductoAdminDto agregarImagen(long id, byte[] contenido, String textoAlternativo) {
        var producto = buscar(id);
        if (contenido.length == 0 || contenido.length > TAMANO_MAXIMO_IMAGEN) {
            throw new ConflictoException("La imagen debe pesar entre 1 byte y 5 MB");
        }
        var formato = FormatoImagen.detectar(contenido)
                .orElseThrow(() -> new ConflictoException("La imagen debe ser JPEG, PNG o WebP"));
        var url = archivos.guardar("productos", contenido, formato.extension);
        alRevertir(() -> archivos.eliminar(url));
        int orden = producto.getImagenes().stream().mapToInt(ImagenProducto::getOrden).max().orElse(-1) + 1;
        producto.agregarImagen(new ImagenProducto(producto, url, textoAlternativo, orden));
        productos.flush();
        return guardado(producto);
    }

    @Transactional
    public ProductoAdminDto actualizarImagen(long id, long idImagen, ImagenRequest solicitud) {
        var producto = buscar(id);
        var imagen = imagen(producto, idImagen);
        imagen.actualizar(solicitud.textoAlternativo(), solicitud.orden());
        if (solicitud.principal()) {
            producto.marcarImagenPrincipal(imagen);
        }
        return guardado(producto);
    }

    @Transactional
    public ProductoAdminDto eliminarImagen(long id, long idImagen) {
        var producto = buscar(id);
        var imagen = imagen(producto, idImagen);
        if (producto.getEstado() == EstadoProducto.PUBLICADO && producto.getImagenes().size() == 1) {
            throw new ConflictoException("Un producto publicado necesita al menos una imagen. Pásalo antes a borrador.");
        }
        producto.quitarImagen(imagen);
        var url = imagen.getUrl();
        alConfirmar(() -> archivos.eliminar(url));
        return guardado(producto);
    }

    @Transactional
    public ProductoAdminDto cambiarEstado(long id, EstadoProductoRequest solicitud) {
        var producto = buscar(id);
        if (solicitud.estado() == null || solicitud.estado() == producto.getEstado()) {
            return guardado(producto);
        }
        try {
            switch (solicitud.estado()) {
                case PUBLICADO -> producto.publicar();
                case ARCHIVADO -> producto.archivar();
                case BORRADOR -> producto.volverABorrador();
            }
        } catch (IllegalStateException e) {
            throw new ConflictoException("No se puede publicar: " + e.getMessage() + ".");
        }
        return guardado(producto);
    }

    private Producto buscar(long id) {
        return productos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("El producto no existe"));
    }

    private static ImagenProducto imagen(Producto producto, long idImagen) {
        return producto.getImagenes().stream().filter(i -> i.getId().equals(idImagen)).findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("La imagen no existe"));
    }

    private Administrador administrador(long id) {
        return usuarios.findById(id).filter(Administrador.class::isInstance).map(Administrador.class::cast)
                .orElseThrow(() -> new RecursoNoEncontradoException("El administrador no existe"));
    }

    private static ConflictoException skuRepetido(String sku) {
        return new ConflictoException("El SKU " + sku + " ya existe");
    }

    /** Si la transacción se revierte, el archivo recién guardado se borra para no dejar huérfanos. */
    private static void alRevertir(Runnable accion) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int estado) {
                if (estado == STATUS_ROLLED_BACK) {
                    accion.run();
                }
            }
        });
    }

    /** El archivo solo se borra cuando la baja de la imagen ya quedó confirmada en la base de datos. */
    private static void alConfirmar(Runnable accion) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                accion.run();
            }
        });
    }
}
