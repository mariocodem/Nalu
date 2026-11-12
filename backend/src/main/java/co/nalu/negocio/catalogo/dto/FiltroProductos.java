package co.nalu.negocio.catalogo.dto;

import co.nalu.datos.entidad.EstadoProducto;

/** Filtros del listado de productos del panel ({@code ?q&estado&idCategoria}). */
public record FiltroProductos(String q, EstadoProducto estado, Long idCategoria) {}
