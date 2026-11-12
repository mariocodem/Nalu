import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, input, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, of, switchMap } from 'rxjs';
import { NOMBRES_ESTADO_PEDIDO, PedidosService } from '../../core/api/pedidos.service';
import { erroresDe } from '../../core/api/problema';

@Component({
  selector: 'app-detalle-pedido',
  imports: [RouterLink, CurrencyPipe, DatePipe, FormsModule],
  template: `
    <section class="pagina">
      <p><a routerLink="/cuenta/pedidos">← Mis pedidos</a></p>
      @if (error(); as e) { <p class="aviso" role="alert">{{ e }}</p> }
      @if (pedido(); as p) {
        <h1>Pedido {{ p.numero }}</h1>
        <p>{{ estados[p.estado] }} · {{ p.fecha | date: 'medium' }}</p>
        @if (p.puedePagar) {
          <p><button type="button" class="boton boton--primario" (click)="pagar(p.numero)">Pagar ahora</button></p>
        }
        @if (p.envio; as e) {
          <p class="aviso aviso--exito">Enviado con {{ e.transportadora }} · guía <strong>{{ e.numeroGuia }}</strong></p>
        }
        <div class="tabla-contenedor panel-seccion">
          <table class="tabla">
            <thead><tr><th scope="col">Pieza</th><th scope="col">Cantidad</th><th scope="col">Precio</th><th scope="col">Subtotal</th></tr></thead>
            <tbody>
              @for (l of p.lineas; track $index) {
                <tr>
                  <td><a [routerLink]="['/producto', l.slug]">{{ l.nombre }}</a>@if (l.talla) { · talla {{ l.talla }} }
                    @if (p.estado === 'ENTREGADO') {
                      @if (resenadas().has(l.slug)) { <span class="nota"> · ¡Gracias por tu reseña!</span> }
                      @else if (resenando() === l.slug) {
                        <form class="resena" (ngSubmit)="enviarResena(l.slug)">
                          <label [for]="'cal-' + $index">Calificación</label>
                          <select [id]="'cal-' + $index" name="calificacion" [(ngModel)]="calificacion">
                            @for (n of [5, 4, 3, 2, 1]; track n) { <option [ngValue]="n">{{ n }} ★</option> }
                          </select>
                          <label [for]="'com-' + $index">Comentario (opcional)</label>
                          <textarea [id]="'com-' + $index" name="comentario" [(ngModel)]="comentario" maxlength="1000"></textarea>
                          <button type="submit" class="boton boton--primario boton--pequeno">Enviar reseña</button>
                        </form>
                      } @else {
                        <button type="button" class="boton boton--enlace" (click)="resenando.set(l.slug)">Escribir reseña</button>
                      }
                    }
                  </td>
                  <td>{{ l.cantidad }}</td>
                  <td>{{ l.precioUnitario | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</td>
                  <td>{{ l.subtotal | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</td>
                </tr>
              }
            </tbody>
          </table>
          <p>Subtotal {{ p.subtotal | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}
            @if (p.descuento > 0) { · Descuento − {{ p.descuento | currency: 'COP' : 'symbol-narrow' : '1.0-0' }} ({{ p.cupon }}) }
            · Envío {{ p.costoEnvio | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}
            · <strong>Total {{ p.total | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</strong></p>
        </div>
        <section class="panel-seccion">
          <h2>Envío a</h2>
          <p>{{ p.direccion.destinatario }} · {{ p.direccion.telefono }}<br />{{ p.direccion.direccion }}, {{ p.direccion.ciudad }}, {{ p.direccion.departamento }}</p>
        </section>
        <section class="panel-seccion">
          <h2>Seguimiento</h2>
          <ol>@for (h of p.historial; track $index) { <li>{{ estados[h.estado] }} · {{ h.fecha | date: 'medium' }}@if (h.nota) { — {{ h.nota }} }</li> }</ol>
        </section>
      }
    </section>
  `,
  styles: `.resena { display: grid; gap: 0.4rem; max-width: 24rem; margin-top: 0.5rem; }
    .resena select, .resena textarea { background: var(--fondo-2); color: var(--tinta); border: 1px solid var(--regla); padding: 0.4rem; font: inherit; }`,
})
export class DetallePedido {
  readonly numero = input.required<string>();
  private readonly pedidos = inject(PedidosService);
  protected readonly estados = NOMBRES_ESTADO_PEDIDO;
  protected readonly error = signal<string | null>(null);
  protected readonly pedido = toSignal(
    toObservable(this.numero).pipe(
      switchMap((n) => this.pedidos.detalle(n).pipe(catchError((e: unknown) => {
        this.error.set(erroresDe(e).general);
        return of(null);
      }))),
    ),
  );

  protected readonly resenando = signal<string | null>(null);
  protected readonly resenadas = signal(new Set<string>());
  protected calificacion = 5;
  protected comentario = '';

  protected enviarResena(slug: string): void {
    this.pedidos.resenar(slug, this.calificacion, this.comentario.trim() || null).subscribe({
      next: () => {
        this.resenadas.update((s) => new Set(s).add(slug));
        this.resenando.set(null);
        this.comentario = '';
      },
      error: (e: unknown) => this.error.set(erroresDe(e).general),
    });
  }

  protected pagar(numero: string): void {
    this.pedidos.reintentarPago(numero).subscribe({
      next: (pago) => window.location.assign(pago.urlPago),
      error: (e: unknown) => this.error.set(erroresDe(e).general),
    });
  }
}
