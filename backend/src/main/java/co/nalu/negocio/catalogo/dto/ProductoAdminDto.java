package co.nalu.negocio.catalogo.dto;

import co.nalu.datos.entidad.EstadoProducto;
import co.nalu.datos.entidad.RolMaterial;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ProductoAdminDto(Long id, String sku, String nombre, String slug, String descripcion, Long idCategoria,
                               BigDecimal precioBase, EstadoProducto estado, OffsetDateTime fechaCreacion,
                               List<MaterialProducto> materiales, List<Variante> variantes, List<Imagen> imagenes) {

    public record MaterialProducto(Long idMaterial, String tipo, String ley, RolMaterial rol, BigDecimal pesoGramos,
                                   BigDecimal quilates) {}

    public record Variante(Long id, String sku, String talla, BigDecimal pesoGramos, BigDecimal precioAdicional,
                           int stockActual, int stockReservado, boolean activa) {}

    public record Imagen(Long id, String url, String textoAlternativo, int orden, boolean principal) {}
}
