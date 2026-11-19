import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, input, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PagosService } from '../../core/api/pagos.service';
import { PedidoDetalle, PedidosService } from '../../core/api/pedidos.service';
import { erroresDe } from '../../core/api/problema';

const INTERVALO_MS = 3000;
const INTENTOS = 20;

/** Mensajes de estado que PayU envía en inglés; otros textos se muestran tal como llegan. */
const MENSAJES_PAYU: Record<string, string> = {
  APPROVED: 'Aprobada', DECLINED: 'Rechazada por la entidad financiera', EXPIRED: 'Expirada',
  PENDING: 'Pendiente', ERROR: 'Error en la transacción',
};

/**
 * Vuelta desde la pasarela.
 * - PayU (ADR 006): la URL trae el resultado firmado (referenceCode, transactionState, TX_VALUE,
 *   signature…). Se reenvía al backend, que verifica la firma y lo registra; sin página de
 *   confirmación PayU no vuelve a avisar, así que no se sondea.
 * - Wompi: la redirección no prueba nada (plan §6.4); se consulta el pedido hasta que el webhook
 *   confirme el resultado.
 */
@Component({
  selector: 'app-resultado-pago',
  imports: [RouterLink, CurrencyPipe],
  template: `
    <section class="pagina pagina--estrecha" aria-live="polite">
      @if (pedido(); as p) {
        @switch (situacion()) {
          @case ('aprobado') {
            <h1>¡Gracias por tu compra!</h1>
            <p>Confirmamos el pago del pedido <strong>{{ p.numero }}</strong> por {{ p.total | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}. Te enviamos un correo con los detalles.</p>
          }
          @case ('rechazado') {
            <h1>El pago no se completó</h1>
            <p>No se cobró nada. Puedes intentarlo de nuevo: si las piezas siguen disponibles, las apartamos otra vez.</p>
            @if (mensajePasarela(); as m) { <p class="nota">Respuesta de la pasarela: {{ m }}</p> }
            <button type="button" class="boton boton--primario" (click)="reintentar(p.numero)" [disabled]="reintentando()">Intentar de nuevo</button>
          }
          @default {
            <h1>Estamos confirmando tu pago</h1>
            <p class="nota">Pedido {{ p.numero }}. Algunos medios de pago, como PSE o el pago en efectivo, pueden tardar en confirmarse. Puedes revisar el estado en el pedido.</p>
          }
        }
        @if (error(); as e) { <p class="aviso" role="alert">{{ e }}</p> }
        <p><a [routerLink]="['/cuenta/pedidos', p.numero]">Ver el pedido</a></p>
      } @else if (error()) {
        <h1>No pudimos consultar el pago</h1>
        <p class="aviso" role="alert">{{ error() }}</p>
        <p><a routerLink="/cuenta/pedidos">Ver mis pedidos</a></p>
      } @else {
        <h1>Estamos confirmando tu pago</h1>
      }
    </section>
  `,
})
export class ResultadoPago implements OnInit {
  readonly pedidoNumero = input<string>('', { alias: 'pedido' });
  private readonly ruta = inject(ActivatedRoute);
  private readonly pedidos = inject(PedidosService);
  private readonly pagos = inject(PagosService);
  private readonly destruir = inject(DestroyRef);

  protected readonly pedido = signal<PedidoDetalle | null>(null);
  protected readonly situacion = signal<'aprobado' | 'rechazado' | 'pendiente'>('pendiente');
  protected readonly mensajePasarela = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly reintentando = signal(false);
  private temporizador: ReturnType<typeof setTimeout> | undefined;

  ngOnInit(): void {
    this.destruir.onDestroy(() => clearTimeout(this.temporizador));
    const parametros = this.ruta.snapshot.queryParams as Record<string, string>;
    if (parametros['referenceCode']) {
      this.registrarRespuestaPayu(parametros);
    } else {
      this.consultar(this.pedidoNumero(), 0);
    }
  }

  protected reintentar(numero: string): void {
    this.reintentando.set(true);
    this.error.set(null);
    this.pagos.reintentar(numero).subscribe({
      next: (pago) => this.pagos.irAPasarela(pago),
      error: (e: unknown) => {
        this.reintentando.set(false);
        this.error.set(erroresDe(e).general);
      },
    });
  }

  private registrarRespuestaPayu(parametros: Record<string, string>): void {
    const mensaje = parametros['message'];
    this.mensajePasarela.set(mensaje ? (MENSAJES_PAYU[mensaje] ?? mensaje) : null);
    this.pagos.registrarRespuestaPayu(parametros).subscribe({
      next: (r) => this.consultar(r.numero, INTENTOS),
      error: (e: unknown) => this.error.set(erroresDe(e).general),
    });
  }

  /** Lee el pedido y, mientras siga pendiente, vuelve a consultarlo hasta agotar los intentos. */
  private consultar(numero: string, intento: number): void {
    this.pedidos.detalle(numero).subscribe({
      next: (p) => {
        this.pedido.set(p);
        if (p.estado !== 'PENDIENTE_PAGO') {
          this.situacion.set(p.estado === 'CANCELADO' ? 'rechazado' : 'aprobado');
        } else if (p.estadoPago === 'RECHAZADO' || p.estadoPago === 'ANULADO' || p.estadoPago === 'ERROR') {
          this.situacion.set('rechazado');
        } else if (intento < INTENTOS) {
          this.temporizador = setTimeout(() => this.consultar(numero, intento + 1), INTERVALO_MS);
        }
      },
      error: (e: unknown) => this.error.set(erroresDe(e).general),
    });
  }
}
