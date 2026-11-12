import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Direccion, Pagina } from './modelos';

// Contrato: docs/arquitectura/api-fase3.md
export type EstadoPedido = 'PENDIENTE_PAGO' | 'PAGADO' | 'EN_PREPARACION' | 'ENVIADO' | 'ENTREGADO' | 'CANCELADO';
export type EstadoPago = 'INICIADO' | 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'ANULADO' | 'ERROR';

export const NOMBRES_ESTADO_PEDIDO: Record<EstadoPedido, string> = {
  PENDIENTE_PAGO: 'Pendiente de pago',
  PAGADO: 'Pagado',
  EN_PREPARACION: 'En preparación',
  ENVIADO: 'Enviado',
  ENTREGADO: 'Entregado',
  CANCELADO: 'Cancelado',
};

export interface LineaCotizada {
  idVariante: number; nombre: string; talla: string | null; cantidad: number;
  precioUnitario: number; subtotal: number; alcanza: boolean;
}
export interface Cotizacion {
  lineas: LineaCotizada[]; subtotal: number; descuento: number; costoEnvio: number; total: number;
  cupon: { codigo: string; aplicado: boolean; mensaje: string | null } | null; faltantes: string[];
}
export interface DatosPago { urlPago: string; referencia: string; montoCentavos: number; moneda: string }
export interface PedidoCreado { numero: string; estado: EstadoPedido; total: number; reservaExpiraEn: string; pago: DatosPago }
export interface PedidoResumen { numero: string; fecha: string; estado: EstadoPedido; total: number; piezas: number }
export interface LineaPedido {
  nombre: string; talla: string | null; slug: string; imagen: string | null; cantidad: number;
  precioUnitario: number; descuento: number; subtotal: number;
}
export interface PedidoDetalle {
  numero: string; fecha: string; estado: EstadoPedido; lineas: LineaPedido[]; subtotal: number; descuento: number;
  costoEnvio: number; total: number; cupon: string | null; direccion: Direccion;
  historial: { estado: EstadoPedido; fecha: string; nota: string | null }[];
  estadoPago: EstadoPago | null; puedePagar: boolean; reservaExpiraEn: string | null;
  envio: { transportadora: string; numeroGuia: string; fechaDespacho: string | null; fechaEntrega: string | null } | null;
}

@Injectable({ providedIn: 'root' })
export class PedidosService {
  private readonly http = inject(HttpClient);

  cotizar(codigoCupon: string | null): Observable<Cotizacion> {
    return this.http.post<Cotizacion>('/api/v1/checkout/cotizar', { codigoCupon });
  }

  crear(idDireccion: number, codigoCupon: string | null): Observable<PedidoCreado> {
    return this.http.post<PedidoCreado>('/api/v1/pedidos', { idDireccion, codigoCupon });
  }

  reintentarPago(numero: string): Observable<DatosPago> {
    return this.http.post<DatosPago>(`/api/v1/pedidos/${encodeURIComponent(numero)}/reintentar-pago`, null);
  }

  misPedidos(pagina = 0): Observable<Pagina<PedidoResumen>> {
    return this.http.get<Pagina<PedidoResumen>>('/api/v1/cuenta/pedidos', { params: { page: pagina } });
  }

  resenar(slug: string, calificacion: number, comentario: string | null): Observable<void> {
    return this.http.post<void>(`/api/v1/productos/${encodeURIComponent(slug)}/resenas`, { calificacion, comentario });
  }

  detalle(numero: string): Observable<PedidoDetalle> {
    return this.http.get<PedidoDetalle>(`/api/v1/cuenta/pedidos/${encodeURIComponent(numero)}`);
  }
}
