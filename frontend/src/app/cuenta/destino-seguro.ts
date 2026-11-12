/** Solo se aceptan rutas internas (evita redirecciones abiertas como //malo.com). */
export function destinoSeguro(destino: string | null | undefined): string {
  return destino && destino.startsWith('/') && !destino.startsWith('//') && !destino.startsWith('/\\')
    ? destino
    : '/';
}
