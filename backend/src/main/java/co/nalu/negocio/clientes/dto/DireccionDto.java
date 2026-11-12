package co.nalu.negocio.clientes.dto;

public record DireccionDto(Long id, String etiqueta, String destinatario, String departamento, String ciudad,
                           String direccion, String codigoPostal, String telefono) {}
