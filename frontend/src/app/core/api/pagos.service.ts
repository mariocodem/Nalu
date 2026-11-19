import { DOCUMENT } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DatosPago, EstadoPago, EstadoPedido } from './pedidos.service';

// Contrato: docs/arquitectura/api-fase3.md §3 y ADR 006
export interface RespuestaPagoRegistrada { numero: string; estado: EstadoPedido; estadoPago: EstadoPago }

@Injectable({ providedIn: 'root' })
export class PagosService {
  private readonly http = inject(HttpClient);
  private readonly documento = inject(DOCUMENT);

  /** Nuevo intento de pago de un pedido pendiente. */
  reintentar(numero: string): Observable<DatosPago> {
    return this.http.post<DatosPago>(`/api/v1/pedidos/${encodeURIComponent(numero)}/reintentar-pago`, null);
  }

  /**
   * Lleva al cliente a la pasarela. Con campos (PayU), los envía a `urlPago` en un formulario POST
   * oculto creado al vuelo; sin campos (Wompi), basta con ir a la URL. Solo se llama tras un clic,
   * así que siempre corre en el navegador, nunca durante el SSR.
   */
  irAPasarela(pago: DatosPago): void {
    const campos = Object.entries(pago.campos ?? {});
    if (campos.length === 0) {
      this.documento.location.assign(pago.urlPago);
      return;
    }
    const formulario = this.documento.createElement('form');
    formulario.method = 'POST';
    formulario.action = pago.urlPago;
    formulario.hidden = true;
    for (const [nombre, valor] of campos) {
      const campo = this.documento.createElement('input');
      campo.type = 'hidden';
      campo.name = nombre;
      campo.value = valor;
      formulario.append(campo);
    }
    this.documento.body.append(formulario);
    formulario.submit();
  }

  /**
   * Reenvía los parámetros con los que PayU devolvió al cliente a la tienda: el backend verifica la
   * firma, registra el resultado y responde el estado del pedido.
   */
  registrarRespuestaPayu(parametros: Record<string, string>): Observable<RespuestaPagoRegistrada> {
    return this.http.post<RespuestaPagoRegistrada>('/api/v1/pagos/payu/respuesta', parametros);
  }
}
