package co.nalu.negocio.catalogo.dto;

import co.nalu.datos.entidad.EstadoProducto;
import java.math.BigDecimal;

public record ProductoResumenDto(Long id, String sku, String nombre, String slug, String categoria,
                                 BigDecimal precioBase, EstadoProducto estado, int existenciaTotal,
                                 String imagenPrincipal) {}
