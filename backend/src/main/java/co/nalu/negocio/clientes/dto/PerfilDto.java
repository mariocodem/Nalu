package co.nalu.negocio.clientes.dto;

public record PerfilDto(String nombre, String apellido, String correo, String telefono, String documento,
                        boolean aceptaComunicaciones) {}
