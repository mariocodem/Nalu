import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CarritoService, ItemCarrito } from '../../core/api/carrito.service';
import { erroresDe } from '../../core/api/problema';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-bolsa',
  imports: [RouterLink, CurrencyPipe],
  template: `
    <section class="pagina">
      <h1>Tu bolsa</h1>
      @if (error(); as e) { <p class="aviso" role="alert">{{ e }}</p> }
      @if (carrito.carrito().items.length === 0) {
        <p class="nota">Tu bolsa está vacía. <a routerLink="/catalogo">Explora las piezas</a></p>
      } @else {
        <ul class="lineas">
          @for (item of carrito.carrito().items; track item.id) {
            <li class="linea" [class.linea--agotada]="!item.disponible">
              @if (item.imagen) { <img [src]="item.imagen" [alt]="item.textoAlternativo ?? ''" width="96" height="96" /> }
              <div class="linea__datos">
                <a [routerLink]="['/producto', item.slug]">{{ item.nombre }}</a>
                @if (item.talla) { <span class="nota">Talla {{ item.talla }}</span> }
                <span>{{ item.precioUnitario | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</span>
                @if (!item.disponible) { <span class="aviso-linea">Ya no está disponible: quítala para continuar.</span> }
              </div>
              <div class="linea__acciones">
                @if (item.disponible) {
                  <label class="sr" [for]="'cant-' + item.id">Cantidad de {{ item.nombre }}</label>
                  <select [id]="'cant-' + item.id" [value]="item.cantidad" (change)="cambiar(item, $any($event.target).value)">
                    @for (n of opciones(item); track n) { <option [value]="n" [selected]="n === item.cantidad">{{ n }}</option> }
                  </select>
                }
                <strong>{{ item.subtotal | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</strong>
                <button type="button" class="boton boton--enlace" (click)="quitar(item)">Quitar</button>
              </div>
            </li>
          }
        </ul>
        <div class="resumen">
          <p>Subtotal <strong>{{ carrito.carrito().subtotal | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</strong></p>
          <p class="nota">El envío y los cupones se calculan en el siguiente paso.</p>
          <button type="button" class="boton boton--primario" [disabled]="hayAgotadas()" (click)="continuar()">Finalizar compra</button>
        </div>
      }
    </section>
  `,
  styles: `
    .lineas { list-style: none; margin: 0 0 2rem; padding: 0; display: grid; gap: 1rem; }
    .linea { display: grid; grid-template-columns: auto 1fr; gap: 1rem; padding: 1rem 0; border-bottom: 1px solid var(--regla-suave); }
    .linea img { width: 96px; height: 96px; object-fit: cover; background: var(--piedra); }
    .linea__datos { display: grid; gap: 0.25rem; align-content: start; }
    .linea__acciones { grid-column: 1 / -1; display: flex; align-items: center; gap: 1rem; justify-content: flex-end; }
    .linea__acciones select { min-height: 40px; padding: 0.3rem 0.6rem; background: var(--fondo-2); color: var(--tinta); border: 1px solid var(--regla); }
    @media (min-width: 48rem) { .linea { grid-template-columns: auto 1fr auto; } .linea__acciones { grid-column: auto; } }
    .linea--agotada { opacity: 0.7; }
    .aviso-linea { color: var(--error); font-size: 0.9rem; }
    .resumen { display: grid; gap: 0.5rem; justify-items: end; text-align: right; }
    .resumen p { margin: 0; }
  `,
})
export class Bolsa {
  protected readonly carrito = inject(CarritoService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly error = signal<string | null>(null);

  protected opciones(item: ItemCarrito): number[] {
    const tope = Math.max(item.maximo, item.cantidad);
    return Array.from({ length: tope }, (_, i) => i + 1);
  }

  protected hayAgotadas(): boolean {
    return this.carrito.carrito().items.some((i) => !i.disponible);
  }

  protected cambiar(item: ItemCarrito, valor: string): void {
    this.error.set(null);
    this.carrito.cambiarCantidad(item.id, Number(valor)).subscribe({ error: (e: unknown) => this.error.set(erroresDe(e).general) });
  }

  protected quitar(item: ItemCarrito): void {
    this.carrito.quitar(item.id).subscribe({ error: (e: unknown) => this.error.set(erroresDe(e).general) });
  }

  protected continuar(): void {
    if (this.auth.usuario()?.roles.includes('CLIENTE')) {
      void this.router.navigateByUrl('/checkout');
    } else {
      void this.router.navigate(['/cuenta/login'], { queryParams: { redirect: '/checkout' } });
    }
  }
}
