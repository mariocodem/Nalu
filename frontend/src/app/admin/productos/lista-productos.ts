import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CatalogoAdminService } from '../../core/api/catalogo-admin.service';
import { EstadoProducto, NOMBRES_ESTADO, Pagina, ProductoResumen } from '../../core/api/modelos';
import { erroresDe } from '../../core/api/problema';

@Component({
  selector: 'app-lista-productos',
  imports: [FormsModule, RouterLink, CurrencyPipe],
  template: `
    <section class="pagina">
      <div class="encabezado">
        <h1>Productos</h1>
        <a class="boton boton--primario" routerLink="nuevo">Nuevo producto</a>
      </div>
      <form class="filtros" (ngSubmit)="buscar()" role="search">
        <div class="campo">
          <label for="f-q">Buscar por nombre o SKU</label>
          <input id="f-q" name="q" [(ngModel)]="q" />
        </div>
        <div class="campo">
          <label for="f-estado">Estado</label>
          <select id="f-estado" name="estado" [(ngModel)]="estado" (ngModelChange)="buscar()">
            <option value="">Todos</option>
            @for (e of estados; track e) { <option [value]="e">{{ nombresEstado[e] }}</option> }
          </select>
        </div>
        <button class="boton boton--secundario" type="submit">Buscar</button>
      </form>
      @if (error(); as e) { <p class="aviso" role="alert">{{ e }}</p> }
      <div class="tabla-contenedor panel-seccion">
        <table class="tabla">
          <thead><tr>
            <th scope="col"><span class="sr">Imagen</span></th><th scope="col">Producto</th><th scope="col">Categoría</th>
            <th scope="col">Precio base</th><th scope="col">Existencia</th><th scope="col">Estado</th>
          </tr></thead>
          <tbody>
            @for (p of pagina()?.contenido ?? []; track p.id) {
              <tr>
                <td>@if (p.imagenPrincipal) { <img [src]="p.imagenPrincipal" alt="" width="48" height="48" class="miniatura" /> }</td>
                <td><a [routerLink]="[p.id]">{{ p.nombre }}</a><br /><span class="nota">{{ p.sku }}</span></td>
                <td>{{ p.categoria }}</td>
                <td>{{ p.precioBase | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</td>
                <td>{{ p.existenciaTotal }}</td>
                <td><span class="etiqueta-estado" [class]="'etiqueta-estado etiqueta-estado--' + p.estado.toLowerCase()">{{ nombresEstado[p.estado] }}</span></td>
              </tr>
            } @empty {
              <tr><td colspan="6" class="nota">No hay productos que coincidan.</td></tr>
            }
          </tbody>
        </table>
      </div>
      @if ((pagina()?.totalPaginas ?? 0) > 1) {
        <nav class="acciones paginacion" aria-label="Paginación">
          <button class="boton boton--secundario boton--pequeno" type="button" [disabled]="numero() === 0" (click)="ir(numero() - 1)">Anterior</button>
          <span>Página {{ numero() + 1 }} de {{ pagina()?.totalPaginas }}</span>
          <button class="boton boton--secundario boton--pequeno" type="button"
                  [disabled]="numero() + 1 >= (pagina()?.totalPaginas ?? 0)" (click)="ir(numero() + 1)">Siguiente</button>
        </nav>
      }
    </section>
  `,
  styles: `
    .encabezado { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 1rem; margin-bottom: 1rem; }
    .encabezado h1 { margin: 0; }
    .filtros { display: flex; flex-wrap: wrap; gap: 0.75rem; align-items: flex-end; margin-bottom: 1rem; }
    .filtros .campo { min-width: 12rem; flex: 1; }
    .miniatura { object-fit: cover; border-radius: 2px; }
    .paginacion { margin-top: 1rem; justify-content: center; }
  `,
})
export class ListaProductos implements OnInit {
  private readonly catalogo = inject(CatalogoAdminService);
  protected readonly estados: EstadoProducto[] = ['BORRADOR', 'PUBLICADO', 'ARCHIVADO'];
  protected readonly nombresEstado = NOMBRES_ESTADO;
  protected readonly pagina = signal<Pagina<ProductoResumen> | null>(null);
  protected readonly numero = signal(0);
  protected readonly error = signal<string | null>(null);
  protected q = '';
  protected estado: EstadoProducto | '' = '';

  ngOnInit(): void {
    this.cargar();
  }

  protected buscar(): void {
    this.numero.set(0);
    this.cargar();
  }

  protected ir(pagina: number): void {
    this.numero.set(pagina);
    this.cargar();
  }

  private cargar(): void {
    this.error.set(null);
    this.catalogo.productos({ q: this.q.trim(), estado: this.estado, page: this.numero() }).subscribe({
      next: (p) => this.pagina.set(p),
      error: (e: unknown) => this.error.set(erroresDe(e).general),
    });
  }
}
