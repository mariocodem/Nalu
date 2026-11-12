import { CurrencyPipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProductoTarjeta } from '../../core/api/tienda.service';

/** Tarjeta de una pieza: fotografía sobre piedra y, debajo, nombre, material y precio desde. */
@Component({
  selector: 'app-vitrina-pieza',
  imports: [RouterLink, CurrencyPipe],
  template: `
    <a class="pieza" [class.pieza--sobre-color]="sobreColor()" [routerLink]="['/producto', producto().slug]">
      <div class="pieza__foto">
        @if (producto().imagen; as img) {
          <img [src]="img.url" [alt]="img.textoAlternativo" loading="lazy" width="640" height="800" />
        } @else {
          <span class="pieza__sin-foto">Fotografía pendiente</span>
        }
        @if (producto().agotado) { <span class="pieza__agotado">Agotada</span> }
      </div>
      <div class="pieza__datos">
        <h3 class="pieza__nombre">{{ producto().nombre }}</h3>
        @if (producto().material) { <p class="pieza__material">{{ producto().material }}</p> }
        @if (producto().precioDesde !== null) {
          <p class="pieza__precio">Desde {{ producto().precioDesde | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</p>
        }
      </div>
    </a>
  `,
  styles: `
    :host { display: block; }
    .pieza { display: grid; gap: 0.9rem; text-decoration: none; color: inherit; }
    .pieza__foto {
      position: relative; aspect-ratio: 4 / 5; overflow: hidden; background: var(--piedra);
    }
    .pieza__foto img {
      width: 100%; height: 100%; object-fit: cover;
      transition: transform 0.9s var(--ease);
    }
    .pieza:hover img { transform: scale(1.035); }
    .pieza__sin-foto { position: absolute; inset: 0; display: grid; place-items: center; color: var(--tinta-3); font-size: 0.85rem; }
    .pieza__agotado {
      position: absolute; top: 0.75rem; left: 0.75rem; padding: 0.25rem 0.6rem;
      background: var(--fondo); color: var(--tinta); font-size: 0.72rem; font-weight: 600; letter-spacing: 0.06em; text-transform: uppercase;
    }
    .pieza__datos { display: grid; gap: 0.15rem; }
    .pieza__nombre {
      margin: 0; font-family: var(--f-texto); font-size: 0.98rem; font-weight: 500; line-height: 1.35; letter-spacing: 0;
      text-underline-offset: 0.25em;
    }
    .pieza:hover .pieza__nombre { text-decoration: underline; text-decoration-thickness: 1px; }
    .pieza__material { margin: 0; color: var(--tinta-3); font-size: 0.88rem; }
    .pieza__precio { margin: 0.25rem 0 0; font-size: 0.95rem; font-variant-numeric: tabular-nums; }
    .pieza--sobre-color .pieza__foto { background: var(--fondo); }
    .pieza--sobre-color .pieza__material { color: var(--sobre-esmeralda-2); }
  `,
})
export class VitrinaPieza {
  readonly producto = input.required<ProductoTarjeta>();
  /** La tarjeta se apoya sobre una franja de color (sala Esmeralda): ajusta los tonos secundarios. */
  readonly sobreColor = input(false);
}
