/**
 * Tipos de la sesión compartidos con el contrato de la API de autenticación
 * (docs/arquitectura/api-autenticacion.md).
 */

/** Roles que puede tener una cuenta. */
export type Rol = 'CLIENTE' | 'ADMINISTRADOR';

/** Datos públicos del usuario autenticado. */
export interface UsuarioSesion {
  id: number;
  nombre: string;
  apellido: string;
  correo: string;
  roles: Rol[];
}

/** Respuesta de login y refresh: token en memoria y datos del usuario. */
export interface Sesion {
  /** JWT HS256 con 15 minutos de vida; solo se guarda en memoria. */
  accessToken: string;
  /** Instante ISO-8601 en UTC en el que expira el access token. */
  expiraEn: string;
  usuario: UsuarioSesion;
}
