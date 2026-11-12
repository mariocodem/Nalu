import { HttpErrorResponse } from '@angular/common/http';

/** Cuerpo `application/problem+json` de la API (RFC 9457). */
export interface Problema {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  errores?: { campo: string; mensaje: string }[];
}

export interface ErroresFormulario {
  general: string | null;
  campos: Record<string, string>;
}

export const SIN_CONEXION =
  'No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.';
const INESPERADO = 'Ocurrió un error inesperado. Inténtalo de nuevo.';

/** Traduce un error HTTP a mensajes para el formulario: generales y por campo. */
export function erroresDe(error: unknown): ErroresFormulario {
  if (!(error instanceof HttpErrorResponse)) {
    return { general: INESPERADO, campos: {} };
  }
  if (error.status === 0) {
    return { general: SIN_CONEXION, campos: {} };
  }
  const problema = (typeof error.error === 'object' && error.error) as Problema | null;
  const campos: Record<string, string> = {};
  for (const e of problema?.errores ?? []) {
    if (e.campo && !campos[e.campo]) {
      campos[e.campo] = e.mensaje;
    }
  }
  const sinCampos = Object.keys(campos).length === 0;
  const general = sinCampos ? (problema?.detail ?? INESPERADO) : null;
  return { general, campos };
}
