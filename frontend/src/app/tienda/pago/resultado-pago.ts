import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PedidoDetalle, PedidosService } from '../../core/api/pedidos.service';
import { erroresDe } from '../../core/api/problema';

const INTERVALO_MS = 3000;
const INTENTOS = 20;

/**
 * Vuelta desde Wompi. La redirección no prueba nada (plan §6.4): se consulta el pedido hasta que
 * el webhook confirme el resultado.
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
            <p>No se cobró nada. Puedes intentarlo de nuevo; tus piezas siguen apartadas mientras haya existencias.</p>
            <button type="button" class="boton boton--primario" (click)="reintentar()" [disabled]="reintentando()">Intentar de nuevo</button>
          }
          @default {
            <h1>Estamos confirmando tu pago</h1>
            <p class="nota">Pedido {{ p.numero }}. Esto puede tardar unos segundos; algunos medios, como PSE, pueden tardar más. Te avisaremos por correo.</p>
          }
        }
        <p><a [routerLink]="['/cuenta/pedidos', p.numero]">Ver el pedido</a></p>
      } @else if (error()) {
        <h1>No encontramos el pedido</h1>
        <p class="aviso" role="alert">{{ error() }}</p>
      } @else {
        <h1>Estamos confirmando tu pago</h1>
      }
    </section>
  `,
})
export class ResultadoPago implements OnInit {
  readonly pedidoNumero = input<string>('', { alias: 'pedido' });
  private readonly pedidos = inject(PedidosService);
  private readonly destruir = inject(DestroyRef);

  protected readonly pedido = signal<PedidoDetalle | null>(null);
  protected readonly situacion = signal<'aprobado' | 'rechazado' | 'pendiente'>('pendiente');
  protected readonly error = signal<string | null>(null);
  protected readonly reintentando = signal(false);
  private temporizador: ReturnType<typeof setTimeout> | undefined;

  ngOnInit(): void {
    this.destruir.onDestroy(() => clearTimeout(this.temporizador));
    this.consultar(0);
  }

  protected reintentar(): void {
    this.reintentando.set(true);
    this.pedidos.reintentarPago(this.pedidoNumero()).subscribe({
      next: (pago) => window.location.assign(pago.urlPago),
      error: (e: unknown) => {
        this.reintentando.set(false);
        this.error.set(erroresDe(e).general);
      },
    });
  }

  private consultar(intento: number): void {
    this.pedidos.detalle(this.pedidoNumero()).subscribe({
      next: (p) => {
        this.pedido.set(p);
        if (p.estado !== 'PENDIENTE_PAGO') {
          this.situacion.set(p.estado === 'CANCELADO' ? 'rechazado' : 'aprobado');
        } else if (p.estadoPago === 'RECHAZADO' || p.estadoPago === 'ANULADO' || p.estadoPago === 'ERROR') {
          this.situacion.set('rechazado');
        } else if (intento < INTENTOS) {
          this.temporizador = setTimeout(() => this.consultar(intento + 1), INTERVALO_MS);
        }
      },
      error: (e: unknown) => this.error.set(erroresDe(e).general),
    });
  }
}
