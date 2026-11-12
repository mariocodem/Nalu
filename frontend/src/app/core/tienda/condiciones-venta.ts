/**
 * Condiciones de venta que la tienda comunica (aviso, beneficios). Solo se muestran: el backend
 * calcula el envío real en la cotización. Fuente: ADR 003 (`NALU_ENVIO_TARIFA`,
 * `NALU_ENVIO_GRATIS_DESDE`, `RESERVA_TTL_MINUTOS`). No hay un endpoint público que las exponga;
 * si cambian en el backend, cambian aquí.
 */
export const CONDICIONES_VENTA = {
  envioTarifaPlana: 15000,
  envioGratisDesde: 200000,
  reservaMinutos: 30,
} as const;
