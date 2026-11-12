import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PiezaSugerida } from '../../core/api/asistente.service';
import { CarritoService } from '../../core/api/carrito.service';
import { erroresDe } from '../../core/api/problema';

/**
 * Pieza recomendada dentro del chat. Precio, tallas y disponibilidad vienen del catálogo; añadir
 * a la bolsa usa el mismo flujo que la ficha y solo ocurre cuando la persona lo pulsa.
 */
@Component({
  selector: 'app-tarjeta-asistente',
  imports: [CurrencyPipe, RouterLink],
  template: `
    <article class="tarjeta" [attr.aria-label]="pieza().nombre">
      <a class="tarjeta__foto" [routerLink]="['/producto', pieza().slug]" tabindex="-1" aria-hidden="true">
        @if (pieza().imagen; as img) { <img [src]="img.url" alt="" width="88" height="110" loading="lazy" /> }
      </a>
      <div class="tarjeta__datos">
        <a class="tarjeta__nombre" [routerLink]="['/producto', pieza().slug]">{{ pieza().nombre }}</a>
        @if (pieza().material) { <span class="tarjeta__material">{{ pieza().material }}</span> }
        @if (precio() !== null) {
          <span class="tarjeta__precio">
            @if (!elegida() && variasTallas()) { Desde }
            {{ precio() | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}
          </span>
        }
        @if (pieza().agotado) {
          <span class="tarjeta__nota">Agotada</span>
        } @else {
          <div class="tarjeta__accion">
            @if (variasTallas()) {
              <label class="sr" [for]="'talla-' + pieza().slug + '-' + idUnico">Talla de {{ pieza().nombre }}</label>
              <select [id]="'talla-' + pieza().slug + '-' + idUnico" (change)="elegir($any($event.target).value)">
                <option value="">Talla</option>
                @for (v of pieza().variantes; track v.id) {
                  <option [value]="v.id" [disabled]="!v.disponible">{{ v.talla ?? 'Única' }}{{ v.disponible ? '' : ' · agotada' }}</option>
                }
              </select>
            }
            <button type="button" class="tarjeta__boton" (click)="agregar()" [disabled]="estado() === 'agregando'">
              {{ estado() === 'agregando' ? 'Añadiendo…' : 'Añadir a la bolsa' }}
            </button>
          </div>
          @if (mensaje(); as m) {
            <p class="tarjeta__mensaje" [class.tarjeta__mensaje--error]="estado() === 'error'" role="status">{{ m }}</p>
          }
        }
      </div>
    </article>
  `,
  styles: `
    :host { display: block; }
    .tarjeta {
      display: grid; grid-template-columns: 88px 1fr; gap: 0.85rem; padding: 0.75rem;
      background: var(--fondo); border: 1px solid var(--regla-suave);
    }
    .tarjeta__foto { display: block; width: 88px; aspect-ratio: 4 / 5; background: var(--piedra); overflow: hidden; }
    .tarjeta__foto img { width: 100%; height: 100%; object-fit: cover; }
    .tarjeta__datos { display: grid; gap: 0.15rem; align-content: start; min-width: 0; }
    .tarjeta__nombre { font-weight: 600; font-size: 0.92rem; line-height: 1.3; text-decoration: none; }
    .tarjeta__nombre:hover { text-decoration: underline; }
    .tarjeta__material, .tarjeta__nota { color: var(--tinta-3); font-size: 0.82rem; }
    .tarjeta__precio { font-size: 0.9rem; font-variant-numeric: tabular-nums; }
    .tarjeta__accion { display: flex; flex-wrap: wrap; gap: 0.4rem; margin-top: 0.45rem; }
    select {
      min-height: 36px; padding: 0 0.5rem; border: 1px solid var(--regla); border-radius: 0;
      background: var(--fondo); color: var(--tinta); font: inherit; font-size: 0.85rem;
    }
    .tarjeta__boton {
      min-height: 36px; padding: 0 0.85rem; border: 0; background: var(--tinta); color: var(--fondo);
      font: inherit; font-size: 0.85rem; font-weight: 600; cursor: pointer;
      transition: background 0.25s var(--ease);
    }
    .tarjeta__boton:hover:not(:disabled) { background: var(--tinta-hover); }
    .tarjeta__boton:disabled { opacity: 0.5; cursor: progress; }
    .tarjeta__mensaje { margin: 0.3rem 0 0; font-size: 0.82rem; color: var(--esmeralda); }
    .tarjeta__mensaje--error { color: var(--error); }
  `,
})
export class TarjetaAsistente {
  private readonly carrito = inject(CarritoService);
  private static contador = 0;

  readonly pieza = input.required<PiezaSugerida>();
  protected readonly idUnico = ++TarjetaAsistente.contador;
  protected readonly elegida = signal<number | null>(null);
  protected readonly estado = signal<'listo' | 'agregando' | 'ok' | 'error'>('listo');
  protected readonly mensaje = signal<string | null>(null);

  protected readonly disponibles = computed(() => this.pieza().variantes.filter((v) => v.disponible));
  protected readonly variasTallas = computed(() => this.pieza().variantes.length > 1);
  protected readonly precio = computed(() => {
    const id = this.elegida();
    return this.pieza().variantes.find((v) => v.id === id)?.precio ?? this.pieza().precioDesde;
  });

  protected elegir(valor: string): void {
    this.elegida.set(valor ? Number(valor) : null);
    this.mensaje.set(null);
    this.estado.set('listo');
  }

  protected agregar(): void {
    const id = this.variasTallas() ? this.elegida() : (this.disponibles()[0]?.id ?? null);
    if (id === null) {
      this.estado.set('error');
      this.mensaje.set('Elige una talla.');
      return;
    }
    this.estado.set('agregando');
    this.mensaje.set(null);
    this.carrito.agregar(id).subscribe({
      next: () => {
        this.estado.set('ok');
        this.mensaje.set('Añadida a tu bolsa.');
      },
      error: (e: unknown) => {
        this.estado.set('error');
        this.mensaje.set(erroresDe(e).general ?? 'No se pudo añadir.');
      },
    });
  }
}
