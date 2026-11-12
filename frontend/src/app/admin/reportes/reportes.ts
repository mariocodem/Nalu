import { CurrencyPipe, KeyValuePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { OperacionAdminService, TablaReporte, TipoReporte } from '../../core/api/operacion-admin.service';
import { erroresDe } from '../../core/api/problema';

const NOMBRES: Record<TipoReporte, string> = {
  ventas: 'Ventas por material', rotacion: 'Piezas más vendidas', agotados: 'Agotados', cupones: 'Uso de cupones', pagos: 'Pagos por estado',
};
const RESUMEN: Record<string, string> = {
  pedidos: 'Pedidos', ingresos: 'Ingresos', descuentos: 'Descuentos', envios: 'Envíos cobrados', ticketPromedio: 'Ticket promedio',
};

/** RF15: reportes por rango de fechas, en pantalla y en CSV. */
@Component({
  selector: 'app-reportes',
  imports: [FormsModule, CurrencyPipe, KeyValuePipe],
  template: `
    <section class="pagina">
      <h1>Reportes</h1>
      <form class="filtros" (ngSubmit)="ver()">
        <div class="campo"><label for="r-tipo">Reporte</label>
          <select id="r-tipo" name="tipo" [(ngModel)]="tipo">@for (t of tipos; track t) { <option [value]="t">{{ nombres[t] }}</option> }</select></div>
        <div class="campo"><label for="r-desde">Desde</label><input id="r-desde" name="desde" type="date" [(ngModel)]="desde" required /></div>
        <div class="campo"><label for="r-hasta">Hasta</label><input id="r-hasta" name="hasta" type="date" [(ngModel)]="hasta" required /></div>
        <button class="boton boton--primario" type="submit">Ver</button>
        <button class="boton boton--secundario" type="button" (click)="descargar()">Descargar CSV</button>
      </form>
      @if (error(); as e) { <p class="aviso" role="alert">{{ e }}</p> }
      @if (tabla(); as t) {
        <section class="panel-seccion">
          <h2>{{ t.titulo }}</h2>
          @if ((t.resumen | keyvalue).length) {
            <dl class="resumen">@for (r of t.resumen | keyvalue; track r.key) {
              <div><dt>{{ resumen[r.key] ?? r.key }}</dt><dd>{{ r.key === 'pedidos' ? r.value : (r.value | currency: 'COP' : 'symbol-narrow' : '1.0-0') }}</dd></div>
            }</dl>
          }
          <div class="tabla-contenedor">
            <table class="tabla">
              <thead><tr>@for (c of t.columnas; track c) { <th scope="col">{{ c }}</th> }</tr></thead>
              <tbody>@for (f of t.filas; track $index) { <tr>@for (v of f; track $index) { <td>{{ v }}</td> }</tr> }
                @empty { <tr><td [attr.colspan]="t.columnas.length" class="nota">Sin datos en el periodo.</td></tr> }</tbody>
            </table>
          </div>
        </section>
      }
    </section>
  `,
  styles: `.filtros { display: flex; flex-wrap: wrap; gap: 0.75rem; align-items: flex-end; margin-bottom: 1rem; } .filtros .campo { min-width: 10rem; }
    .resumen { display: flex; flex-wrap: wrap; gap: 1.5rem; margin: 0 0 1rem; } .resumen dt { color: var(--tinta-3); font-size: 0.85rem; } .resumen dd { margin: 0; font-size: 1.25rem; }`,
})
export class Reportes {
  private readonly operacion = inject(OperacionAdminService);
  protected readonly tipos = Object.keys(NOMBRES) as TipoReporte[];
  protected readonly nombres = NOMBRES;
  protected readonly resumen = RESUMEN;
  protected readonly tabla = signal<TablaReporte | null>(null);
  protected readonly error = signal<string | null>(null);
  protected tipo: TipoReporte = 'ventas';
  protected hasta = new Date().toISOString().slice(0, 10);
  protected desde = this.hasta.slice(0, 8) + '01';

  protected ver(): void {
    this.error.set(null);
    this.operacion.reporte(this.tipo, this.desde, this.hasta).subscribe({ next: (t) => this.tabla.set(t), error: (e: unknown) => this.error.set(erroresDe(e).general) });
  }

  protected descargar(): void {
    this.operacion.reporteCsv(this.tipo, this.desde, this.hasta).subscribe({
      next: (archivo) => {
        const url = URL.createObjectURL(archivo);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = `nalu-${this.tipo}-${this.desde}-a-${this.hasta}.csv`;
        enlace.click();
        URL.revokeObjectURL(url);
      },
      error: (e: unknown) => this.error.set(erroresDe(e).general),
    });
  }
}
