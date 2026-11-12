package co.nalu.datos.entidad;

import java.io.Serializable;

/**
 * Clave compuesta de {@link ProductoMaterial}: el producto y el material.
 */
public record ProductoMaterialId(Long idProducto, Long idMaterial) implements Serializable {
}
