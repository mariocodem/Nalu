import { CurrencyPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { CategoriaPublica, TiendaService } from '../../core/api/tienda.service';
import { AuthService } from '../../core/auth/auth.service';
import { CONDICIONES_VENTA } from '../../core/tienda/condiciones-venta';

/** Pie de la tienda: navegación secundaria con rutas existentes y las condiciones de compra vigentes. */
@Component({
  selector: 'app-pie',
  imports: [RouterLink, CurrencyPipe],
  template: `
    <footer class="pie">
      <div class="pie__cuerpo">
        <p class="pie__marca">Nalu</p>
        <nav class="pie__columna" aria-labelledby="pie-tienda">
          <h2 id="pie-tienda">Tienda</h2>
          <ul>
            @for (c of categorias(); track c.slug) {
              <li><a [routerLink]="['/catalogo', c.slug]">{{ c.nombre }}</a></li>
            }
            <li><a routerLink="/catalogo">Todas las piezas</a></li>
          </ul>
        </nav>
        <nav class="pie__columna" aria-labelledby="pie-cuenta">
          <h2 id="pie-cuenta">Tu cuenta</h2>
          <ul>
            @if (auth.usuario()) {
              @if (auth.esAdministrador()) {
                <li><a routerLink="/admin">Panel</a></li>
              } @else {
                <li><a routerLink="/cuenta">Mi cuenta</a></li>
                <li><a routerLink="/cuenta/pedidos">Pedidos</a></li>
              }
            } @else {
              <li><a routerLink="/cuenta/login">Ingresar</a></li>
              <li><a routerLink="/cuenta/registro">Crear cuenta</a></li>
            }
            <li><a routerLink="/bolsa">Bolsa</a></li>
          </ul>
        </nav>
        <div class="pie__columna">
          <h2>Comprar en Nalu</h2>
          <ul>
            <li>Envío a toda Colombia por {{ condiciones.envioTarifaPlana | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</li>
            <li>Gratis desde {{ condiciones.envioGratisDesde | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</li>
            <li>Pago en línea con Wompi</li>
          </ul>
        </div>
      </div>
      <p class="pie__legal">© {{ anio }} Nalu · Joyería en acero, plata y oro · Precios en pesos colombianos · Fotografías de demostración para uso académico</p>
    </footer>
  `,
  styles: `
    :host { display: block; }
    .pie {
      background: var(--esmeralda-profunda);
      color: var(--sobre-esmeralda);
      padding: clamp(3.5rem, 8vw, 6rem) var(--gutter) 2rem;
    }
    .pie__cuerpo {
      display: grid;
      gap: 2.5rem 3rem;
      grid-template-columns: repeat(auto-fit, minmax(min(100%, 11rem), 1fr));
      max-width: var(--ancho);
      margin-inline: auto;
    }
    .pie__marca {
      margin: 0;
      font-family: var(--f-titular);
      font-size: clamp(2.6rem, 5vw, 3.6rem);
      font-weight: 500;
      letter-spacing: 0.3em;
      line-height: 1;
      text-transform: uppercase;
    }
    h2 {
      margin: 0 0 1rem;
      font-family: var(--f-texto);
      font-size: 0.75rem;
      font-weight: 600;
      letter-spacing: 0.12em;
      text-transform: uppercase;
      color: var(--sobre-esmeralda-2);
    }
    ul { display: grid; gap: 0.55rem; margin: 0; padding: 0; list-style: none; font-size: 0.95rem; }
    a { text-decoration: none; }
    a:hover { text-decoration: underline; }
    a:focus-visible { outline-color: var(--sobre-esmeralda); }
    .pie__legal {
      max-width: var(--ancho);
      margin: clamp(3rem, 6vw, 4.5rem) auto 0;
      padding-top: 1.5rem;
      border-top: 1px solid rgb(244 241 234 / 0.16);
      font-size: 0.8rem;
      color: var(--sobre-esmeralda-2);
    }
  `,
})
export class Pie {
  protected readonly auth = inject(AuthService);
  protected readonly condiciones = CONDICIONES_VENTA;
  protected readonly anio = new Date().getFullYear();
  protected readonly categorias = toSignal(inject(TiendaService).categorias().pipe(catchError(() => of([]))), {
    initialValue: [] as CategoriaPublica[],
  });
}
