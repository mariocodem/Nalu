package co.nalu.negocio.cuentas.dto;

import java.util.List;

/** Usuario autenticado tal como lo ve el frontend (docs/arquitectura/api-autenticacion.md). */
public record UsuarioSesionDto(Long id, String nombre, String apellido, String correo, List<String> roles) {}
