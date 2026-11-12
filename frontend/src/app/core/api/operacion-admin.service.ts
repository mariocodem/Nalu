import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Pagina } from './modelos';
import { EstadoPago, EstadoPedido, PedidoDetalle } from './pedidos.service';

// Contrato: docs/arquitectura/api-fase5.md
export interface PedidoAdminResumen {
  numero: string; fecha: string; cliente: string; correo: string; estado: EstadoPedido; total: number; piezas: number;
}
export interface PedidoAdminDetalle {
  pedido: PedidoDetalle;
  cliente: { nombre: string; correo: string; telefono: string | null };
  pagos: { referencia: string; estado: EstadoPago; metodo: string | null; monto: number; referenciaExterna: string | null; fecha: string }[];
  siguientes: EstadoPedido[];
}
export type TipoMovimiento = 'ENTRADA' | 'SALIDA' | 'AJUSTE';
export interface Movimiento {
  id: number; fecha: string; idVariante: number; sku: string; producto: string; talla: string | null;
  tipo: TipoMovimiento; cantidad: number; motivo: string; responsable: string; numeroPedido: string | null;
}
export interface Alerta {
  idVariante: number; idProducto: number; sku: string; producto: string; talla: string | null;
  stockActual: number; stockReservado: number; disponible: number;
}
export type EstadoResena = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA';
export interface ResenaAdmin {
  id: number; producto: string; slug: string; cliente: string; correo: string; calificacion: number;
  comentario: string | null; fecha: string; estado: EstadoResena;
}
export type TipoReporte = 'ventas' | 'rotacion' | 'agotados' | 'cupones' | 'pagos';
export interface TablaReporte { titulo: string; columnas: string[]; filas: (string | number)[][]; resumen: Record<string, number> }

@Injectable({ providedIn: 'root' })
export class OperacionAdminService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/admin';

  pedidos(filtro: { estado?: string; desde?: string; hasta?: string; q?: string; page?: number }): Observable<Pagina<PedidoAdminResumen>> {
    let params = new HttpParams();
    for (const [k, v] of Object.entries(filtro)) if (v !== undefined && v !== null && v !== '') params = params.set(k, String(v));
    return this.http.get<Pagina<PedidoAdminResumen>>(`${this.base}/pedidos`, { params });
  }
  pedido(numero: string): Observable<PedidoAdminDetalle> {
    return this.http.get<PedidoAdminDetalle>(`${this.base}/pedidos/${encodeURIComponent(numero)}`);
  }
  cambiarEstado(numero: string, estado: EstadoPedido, nota: string | null): Observable<PedidoAdminDetalle> {
    return this.http.patch<PedidoAdminDetalle>(`${this.base}/pedidos/${encodeURIComponent(numero)}/estado`, { estado, nota });
  }
  registrarEnvio(numero: string, transportadora: string, numeroGuia: string): Observable<PedidoAdminDetalle> {
    return this.http.post<PedidoAdminDetalle>(`${this.base}/pedidos/${encodeURIComponent(numero)}/envio`, { transportadora, numeroGuia });
  }

  movimientos(idVariante: number | null, page = 0): Observable<Pagina<Movimiento>> {
    let params = new HttpParams().set('page', page);
    if (idVariante) params = params.set('idVariante', idVariante);
    return this.http.get<Pagina<Movimiento>>(`${this.base}/inventario/movimientos`, { params });
  }
  registrarMovimiento(datos: { idVariante: number; tipo: TipoMovimiento; cantidad: number; motivo: string }): Observable<Movimiento> {
    return this.http.post<Movimiento>(`${this.base}/inventario/movimientos`, datos);
  }
  alertas(umbral: number): Observable<Alerta[]> {
    return this.http.get<Alerta[]>(`${this.base}/inventario/alertas`, { params: { umbral } });
  }

  resenas(estado: EstadoResena | ''): Observable<Pagina<ResenaAdmin>> {
    return this.http.get<Pagina<ResenaAdmin>>(`${this.base}/resenas`, { params: estado ? { estado } : {} });
  }
  moderar(id: number, estado: EstadoResena): Observable<ResenaAdmin> {
    return this.http.patch<ResenaAdmin>(`${this.base}/resenas/${id}`, { estado });
  }

  reporte(tipo: TipoReporte, desde: string, hasta: string): Observable<TablaReporte> {
    return this.http.get<TablaReporte>(`${this.base}/reportes/${tipo}`, { params: { desde, hasta } });
  }
  reporteCsv(tipo: TipoReporte, desde: string, hasta: string): Observable<Blob> {
    return this.http.get(`${this.base}/reportes/${tipo}`, { params: { desde, hasta, formato: 'csv' }, responseType: 'blob' });
  }
}
