import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Pagina } from '../../core/api/modelos';
import { OperacionAdminService, PedidoAdminResumen } from '../../core/api/operacion-admin.service';
import { EstadoPedido, NOMBRES_ESTADO_PEDIDO } from '../../core/api/pedidos.service';
import { erroresDe } from '../../core/api/problema';

@Component({
  selector: 'app-pedidos-admin',
  imports: [FormsModule, RouterLink, CurrencyPipe, DatePipe],
  template: `
    <section class="pagina">
      <h1>Pedidos</h1>
      <form class="filtros" (ngSubmit)="buscar(0)" role="search">
        <div class="campo"><label for="f-q">Número o correo</label><input id="f-q" name="q" [(ngModel)]="q" /></div>
        <div class="campo">
          <label for="f-estado">Estado</label>
          <select id="f-estado" name="estado" [(ngModel)]="estado">
            <option value="">Todos</option>
            @for (e of estados; track e) { <option [value]="e">{{ nombres[e] }}</option> }
          </select>
        </div>
        <div class="campo"><label for="f-desde">Desde</label><input id="f-desde" name="desde" type="date" [(ngModel)]="desde" /></div>
        <div class="campo"><label for="f-hasta">Hasta</label><input id="f-hasta" name="hasta" type="date" [(ngModel)]="hasta" /></div>
        <button class="boton boton--secundario" type="submit">Filtrar</button>
      </form>
      @if (error(); as e) { <p class="aviso" role="alert">{{ e }}</p> }
      <div class="tabla-contenedor panel-seccion">
        <table class="tabla">
          <thead><tr><th scope="col">Pedido</th><th scope="col">Fecha</th><th scope="col">Cliente</th><th scope="col">Piezas</th><th scope="col">Total</th><th scope="col">Estado</th></tr></thead>
          <tbody>
            @for (p of pagina()?.contenido ?? []; track p.numero) {
              <tr>
                <td><a [routerLink]="[p.numero]">{{ p.numero }}</a></td>
                <td>{{ p.fecha | date: 'short' }}</td>
                <td>{{ p.cliente }}<br /><span class="nota">{{ p.correo }}</span></td>
                <td>{{ p.piezas }}</td>
                <td>{{ p.total | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</td>
                <td><span class="etiqueta-estado" [class.etiqueta-estado--publicado]="p.estado === 'PAGADO' || p.estado === 'EN_PREPARACION'">{{ nombres[p.estado] }}</span></td>
              </tr>
            } @empty { <tr><td colspan="6" class="nota">No hay pedidos con esos filtros.</td></tr> }
          </tbody>
        </table>
      </div>
      @if ((pagina()?.totalPaginas ?? 0) > 1) {
        <nav class="acciones" aria-label="Paginación">
          <button class="boton boton--secundario boton--pequeno" type="button" [disabled]="(pagina()?.pagina ?? 0) === 0" (click)="buscar((pagina()?.pagina ?? 0) - 1)">Anterior</button>
          <span>Página {{ (pagina()?.pagina ?? 0) + 1 }} de {{ pagina()?.totalPaginas }}</span>
          <button class="boton boton--secundario boton--pequeno" type="button" [disabled]="(pagina()?.pagina ?? 0) + 1 >= (pagina()?.totalPaginas ?? 0)" (click)="buscar((pagina()?.pagina ?? 0) + 1)">Siguiente</button>
        </nav>
      }
    </section>
  `,
  styles: `.filtros { display: flex; flex-wrap: wrap; gap: 0.75rem; align-items: flex-end; margin-bottom: 1rem; } .filtros .campo { min-width: 10rem; flex: 1; }`,
})
export class PedidosAdmin implements OnInit {
  private readonly operacion = inject(OperacionAdminService);
  protected readonly estados = Object.keys(NOMBRES_ESTADO_PEDIDO) as EstadoPedido[];
  protected readonly nombres = NOMBRES_ESTADO_PEDIDO;
  protected readonly pagina = signal<Pagina<PedidoAdminResumen> | null>(null);
  protected readonly error = signal<string | null>(null);
  protected q = '';
  protected estado = '';
  protected desde = '';
  protected hasta = '';

  ngOnInit(): void {
    this.buscar(0);
  }

  protected buscar(page: number): void {
    this.operacion.pedidos({ q: this.q.trim(), estado: this.estado, desde: this.desde, hasta: this.hasta, page }).subscribe({
      next: (p) => this.pagina.set(p),
      error: (e: unknown) => this.error.set(erroresDe(e).general),
    });
  }
}
