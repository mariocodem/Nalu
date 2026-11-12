import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CarritoService } from '../../core/api/carrito.service';
import { CuentaService } from '../../core/api/cuenta.service';
import { Direccion } from '../../core/api/modelos';
import { Cotizacion, PedidosService } from '../../core/api/pedidos.service';
import { erroresDe } from '../../core/api/problema';

/** RF09–RF11: dirección, cupón, totales calculados por el servidor y paso a la pasarela. */
@Component({
  selector: 'app-checkout',
  imports: [FormsModule, RouterLink, CurrencyPipe],
  template: `
    <section class="pagina">
      <h1>Finalizar compra</h1>
      @if (error(); as e) { <p class="aviso" role="alert">{{ e }}</p> }
      @if (faltantes().length) {
        <div class="aviso" role="alert">
          <p>Algunas piezas ya no alcanzan:</p>
          <ul>@for (f of faltantes(); track f) { <li>{{ f }}</li> }</ul>
          <a routerLink="/bolsa">Ajustar la bolsa</a>
        </div>
      }
      <div class="checkout">
        <section class="panel-seccion" aria-labelledby="t-envio">
          <h2 id="t-envio">Envío</h2>
          @if (direcciones().length === 0) {
            <p class="nota">Necesitas una dirección de envío. <a routerLink="/cuenta">Añádela en tu cuenta</a> y vuelve aquí.</p>
          } @else {
            <fieldset class="direcciones">
              <legend class="sr">Dirección de envío</legend>
              @for (d of direcciones(); track d.id) {
                <label class="direccion">
                  <input type="radio" name="direccion" [value]="d.id" [(ngModel)]="idDireccion" />
                  <span><strong>{{ d.etiqueta }}</strong> · {{ d.destinatario }}<br />{{ d.direccion }}, {{ d.ciudad }}, {{ d.departamento }}</span>
                </label>
              }
            </fieldset>
            <p class="nota"><a routerLink="/cuenta">Gestionar direcciones</a></p>
          }
        </section>

        <section class="panel-seccion" aria-labelledby="t-resumen">
          <h2 id="t-resumen">Resumen</h2>
          @if (cotizacion(); as c) {
            <ul class="resumen-lineas">
              @for (l of c.lineas; track l.idVariante) {
                <li><span>{{ l.cantidad }} × {{ l.nombre }}@if (l.talla) { (talla {{ l.talla }}) }</span>
                  <span>{{ l.subtotal | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</span></li>
              }
            </ul>
            <form class="cupon" (ngSubmit)="aplicarCupon()">
              <div class="campo">
                <label for="cupon">Cupón</label>
                <input id="cupon" name="cupon" [(ngModel)]="cupon" maxlength="30" autocomplete="off" />
              </div>
              <button type="submit" class="boton boton--secundario">Aplicar</button>
            </form>
            @if (c.cupon && !c.cupon.aplicado) { <p class="campo__error" role="alert">{{ c.cupon.mensaje }}</p> }
            <dl class="totales">
              <div><dt>Subtotal</dt><dd>{{ c.subtotal | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</dd></div>
              @if (c.descuento > 0) { <div><dt>Descuento ({{ c.cupon?.codigo }})</dt><dd>− {{ c.descuento | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</dd></div> }
              <div><dt>Envío</dt><dd>{{ c.costoEnvio === 0 ? 'Gratis' : (c.costoEnvio | currency: 'COP' : 'symbol-narrow' : '1.0-0') }}</dd></div>
              <div class="totales__total"><dt>Total</dt><dd>{{ c.total | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</dd></div>
            </dl>
            <button type="button" class="boton boton--primario pagar" (click)="pagar()"
                    [disabled]="!idDireccion || enviando() || c.faltantes.length > 0">
              {{ enviando() ? 'Reservando tus piezas…' : 'Pagar con Wompi' }}
            </button>
            <p class="nota">Reservamos tus piezas 30 minutos mientras completas el pago. Pagas en la plataforma segura de Wompi: Nalu no recibe los datos de tu tarjeta.</p>
          } @else if (!error()) {
            <p class="nota">Calculando…</p>
          }
        </section>
      </div>
    </section>
  `,
  styles: `
    .checkout { display: grid; gap: 1.5rem; }
    @media (min-width: 60rem) { .checkout { grid-template-columns: 3fr 2fr; align-items: start; } .checkout .panel-seccion + .panel-seccion { margin-top: 0; } }
    .direcciones { border: 0; padding: 0; margin: 0; display: grid; gap: 0.75rem; }
    .direccion { display: flex; gap: 0.75rem; align-items: flex-start; padding: 0.85rem; border: 1px solid var(--regla-suave); cursor: pointer; }
    .direccion input { accent-color: var(--acento); margin-top: 0.3rem; }
    .resumen-lineas { list-style: none; padding: 0; margin: 0 0 1rem; display: grid; gap: 0.4rem; }
    .resumen-lineas li, .totales div { display: flex; justify-content: space-between; gap: 1rem; }
    .cupon { display: flex; gap: 0.5rem; align-items: flex-end; margin-bottom: 0.5rem; }
    .cupon .campo { flex: 1; }
    .totales { margin: 1rem 0; display: grid; gap: 0.35rem; }
    .totales dd { margin: 0; font-variant-numeric: tabular-nums; }
    .totales__total { padding-top: 0.5rem; border-top: 1px solid var(--regla); font-size: 1.2rem; }
    .pagar { width: 100%; }
  `,
})
export class Checkout implements OnInit {
  private readonly pedidos = inject(PedidosService);
  private readonly cuenta = inject(CuentaService);
  private readonly carrito = inject(CarritoService);

  protected readonly direcciones = signal<Direccion[]>([]);
  protected readonly cotizacion = signal<Cotizacion | null>(null);
  protected readonly faltantes = signal<string[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly enviando = signal(false);
  protected idDireccion: number | null = null;
  protected cupon = '';

  ngOnInit(): void {
    this.cuenta.direcciones().subscribe((d) => {
      this.direcciones.set(d);
      this.idDireccion = d[0]?.id ?? null;
    });
    this.cotizar();
  }

  protected aplicarCupon(): void {
    this.cotizar();
  }

  protected pagar(): void {
    if (!this.idDireccion) return;
    this.error.set(null);
    this.faltantes.set([]);
    this.enviando.set(true);
    const cupon = this.cotizacion()?.cupon?.aplicado ? this.cupon.trim() : null;
    this.pedidos.crear(this.idDireccion, cupon).subscribe({
      next: (p) => {
        this.carrito.vaciarLocal();
        window.location.assign(p.pago.urlPago);
      },
      error: (e: unknown) => {
        this.enviando.set(false);
        const faltantes = (e as { error?: { faltantes?: string[] } }).error?.faltantes;
        if (faltantes?.length) this.faltantes.set(faltantes);
        else this.error.set(erroresDe(e).general);
        this.cotizar();
      },
    });
  }

  private cotizar(): void {
    this.pedidos.cotizar(this.cupon.trim() || null).subscribe({
      next: (c) => {
        this.cotizacion.set(c);
        this.faltantes.set(c.faltantes);
      },
      error: (e: unknown) => this.error.set(erroresDe(e).general),
    });
  }
}
