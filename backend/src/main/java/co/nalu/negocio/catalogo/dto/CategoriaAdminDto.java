package co.nalu.negocio.catalogo.dto;

import java.util.List;

public record CategoriaAdminDto(Long id, String nombre, String slug, String descripcion, boolean activa, Long idPadre,
                                List<CategoriaAdminDto> hijos) {}
