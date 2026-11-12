import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Pagina } from '../../core/api/modelos';
import { Alerta, Movimiento, OperacionAdminService, TipoMovimiento } from '../../core/api/operacion-admin.service';
import { erroresDe } from '../../core/api/problema';

/** RF13: alertas de existencias, movimientos manuales y su historial. */
@Component({
  selector: 'app-inventario',
  imports: [FormsModule, RouterLink, DatePipe],
  template: `
    <section class="pagina">
      <h1>Inventario</h1>
      @if (error(); as e) { <p class="aviso" role="alert">{{ e }}</p> }
      @if (exito(); as e) { <p class="aviso aviso--exito" role="status">{{ e }}</p> }
      <section class="panel-seccion">
        <h2>Pocas existencias</h2>
        <div class="campo umbral"><label for="umbral">Mostrar con disponibles ≤</label>
          <input id="umbral" type="number" min="0" [(ngModel)]="umbral" (change)="cargarAlertas()" /></div>
        <div class="tabla-contenedor">
          <table class="tabla">
            <thead><tr><th scope="col">SKU</th><th scope="col">Pieza</th><th scope="col">Existencia</th><th scope="col">Reservadas</th><th scope="col">Disponibles</th><th scope="col"><span class="sr">Acción</span></th></tr></thead>
            <tbody>@for (a of alertas(); track a.idVariante) {
              <tr><td>{{ a.sku }}</td><td><a [routerLink]="['/admin/productos', a.idProducto]">{{ a.producto }}</a>@if (a.talla) { · {{ a.talla }} }</td>
                <td>{{ a.stockActual }}</td><td>{{ a.stockReservado }}</td><td><strong>{{ a.disponible }}</strong></td>
                <td><button type="button" class="boton boton--secundario boton--pequeno" (click)="elegir(a)">Registrar movimiento</button></td></tr>
            } @empty { <tr><td colspan="6" class="nota">Nada por debajo del umbral.</td></tr> }</tbody>
          </table>
        </div>
      </section>

      <section class="panel-seccion">
        <h2>Registrar movimiento</h2>
        <form class="formulario" (ngSubmit)="registrar()">
          <div class="formulario__fila">
            <div class="campo"><label for="m-variante">Id de variante</label><input id="m-variante" name="variante" type="number" min="1" [(ngModel)]="idVariante" required />
              @if (skuElegido()) { <p class="nota">{{ skuElegido() }}</p> }</div>
            <div class="campo"><label for="m-tipo">Tipo</label>
              <select id="m-tipo" name="tipo" [(ngModel)]="tipo">
                <option value="ENTRADA">Entrada</option><option value="SALIDA">Salida</option><option value="AJUSTE">Ajuste (+/−)</option>
              </select></div>
            <div class="campo"><label for="m-cantidad">Cantidad</label><input id="m-cantidad" name="cantidad" type="number" [(ngModel)]="cantidad" required /></div>
          </div>
          <div class="campo"><label for="m-motivo">Motivo</label><input id="m-motivo" name="motivo" [(ngModel)]="motivo" maxlength="160" required placeholder="Lote nuevo, pieza dañada, conteo físico…" /></div>
          <div class="acciones"><button class="boton boton--primario" type="submit" [disabled]="!idVariante || !cantidad || !motivo.trim()">Registrar</button></div>
        </form>
      </section>

      <section class="panel-seccion">
        <h2>Historial@if (filtro()) { de la variante {{ filtro() }} <button type="button" class="boton boton--enlace" (click)="verTodo()">ver todo</button> }</h2>
        <div class="tabla-contenedor">
          <table class="tabla">
            <thead><tr><th scope="col">Fecha</th><th scope="col">SKU</th><th scope="col">Tipo</th><th scope="col">Cantidad</th><th scope="col">Motivo</th><th scope="col">Responsable</th></tr></thead>
            <tbody>@for (m of movimientos()?.contenido ?? []; track m.id) {
              <tr><td>{{ m.fecha | date: 'short' }}</td><td>{{ m.sku }}</td><td>{{ m.tipo }}</td><td>{{ m.cantidad > 0 ? '+' : '' }}{{ m.cantidad }}</td>
                <td>{{ m.motivo }}</td><td>{{ m.responsable }}@if (m.numeroPedido) { · {{ m.numeroPedido }} }</td></tr>
            } @empty { <tr><td colspan="6" class="nota">Sin movimientos.</td></tr> }</tbody>
          </table>
        </div>
      </section>
    </section>
  `,
  styles: `.umbral { max-width: 14rem; margin-bottom: 0.75rem; }`,
})
export class Inventario implements OnInit {
  readonly variante = input<string>();
  private readonly operacion = inject(OperacionAdminService);
  protected readonly alertas = signal<Alerta[]>([]);
  protected readonly movimientos = signal<Pagina<Movimiento> | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly exito = signal<string | null>(null);
  protected readonly skuElegido = signal<string | null>(null);
  protected readonly filtro = signal<number | null>(null);
  protected umbral = 2;
  protected idVariante: number | null = null;
  protected tipo: TipoMovimiento = 'ENTRADA';
  protected cantidad: number | null = null;
  protected motivo = '';

  ngOnInit(): void {
    const v = Number(this.variante());
    if (v) {
      this.idVariante = v;
      this.filtro.set(v);
    }
    this.cargarAlertas();
    this.cargarMovimientos();
  }

  protected cargarAlertas(): void {
    this.operacion.alertas(this.umbral ?? 0).subscribe((a) => this.alertas.set(a));
  }

  protected elegir(a: Alerta): void {
    this.idVariante = a.idVariante;
    this.skuElegido.set(`${a.sku} · ${a.producto}`);
    this.filtro.set(a.idVariante);
    this.cargarMovimientos();
  }

  protected verTodo(): void {
    this.filtro.set(null);
    this.cargarMovimientos();
  }

  protected registrar(): void {
    this.error.set(null);
    this.exito.set(null);
    this.operacion
      .registrarMovimiento({ idVariante: this.idVariante!, tipo: this.tipo, cantidad: this.cantidad!, motivo: this.motivo.trim() })
      .subscribe({
        next: (m) => {
          this.exito.set(`Movimiento registrado: ${m.sku} ${m.cantidad > 0 ? '+' : ''}${m.cantidad}.`);
          this.cantidad = null;
          this.motivo = '';
          this.cargarAlertas();
          this.cargarMovimientos();
        },
        error: (e: unknown) => {
          const errores = erroresDe(e);
          this.error.set(errores.general ?? Object.values(errores.campos).join(' '));
        },
      });
  }

  private cargarMovimientos(): void {
    this.operacion.movimientos(this.filtro()).subscribe((m) => this.movimientos.set(m));
  }
}
