import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ImagenPublica, VarianteFicha } from './tienda.service';

// Contrato: docs/arquitectura/api-asistente.md
export type RolMensaje = 'cliente' | 'asistente';
export interface MensajeHistorial { rol: RolMensaje; texto: string }
export interface PiezaSugerida {
  slug: string; nombre: string; imagen: ImagenPublica | null; material: string | null;
  precioDesde: number | null; agotado: boolean; variantes: VarianteFicha[];
}
export interface RespuestaAsistente { texto: string; piezas: PiezaSugerida[] }

/** Asistente de compras con IA (ADR 005). El backend llama al proveedor; aquí solo viaja la conversación. */
@Injectable({ providedIn: 'root' })
export class AsistenteService {
  private readonly http = inject(HttpClient);

  estado(): Observable<{ disponible: boolean }> {
    return this.http.get<{ disponible: boolean }>('/api/v1/asistente/estado');
  }

  enviar(historial: MensajeHistorial[], mensaje: string): Observable<RespuestaAsistente> {
    return this.http.post<RespuestaAsistente>('/api/v1/asistente/mensajes', { historial, mensaje });
  }
}
