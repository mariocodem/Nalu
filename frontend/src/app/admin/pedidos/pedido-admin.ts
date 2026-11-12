import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { OperacionAdminService, PedidoAdminDetalle } from '../../core/api/operacion-admin.service';
import { EstadoPedido, NOMBRES_ESTADO_PEDIDO } from '../../core/api/pedidos.service';
import { erroresDe } from '../../core/api/problema';

/** RF14: detalle y avance del pedido según la máquina de estados (plan §6.6). */
@Component({
  selector: 'app-pedido-admin',
  imports: [FormsModule, RouterLink, CurrencyPipe, DatePipe],
  template: `
    <section class="pagina">
      <p><a routerLink="/admin/pedidos">← Pedidos</a></p>
      @if (error(); as e) { <p class="aviso" role="alert">{{ e }}</p> }
      @if (detalle(); as d) {
        <h1>Pedido {{ d.pedido.numero }}</h1>
        <p><span class="etiqueta-estado">{{ nombres[d.pedido.estado] }}</span> · {{ d.pedido.fecha | date: 'medium' }} · {{ d.pedido.total | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</p>

        <section class="panel-seccion">
          <h2>Siguiente paso</h2>
          @if (d.siguientes.length === 0) { <p class="nota">Este pedido está cerrado.</p> }
          @if (d.pedido.estado === 'EN_PREPARACION') {
            <form class="formulario" (ngSubmit)="despachar()">
              <div class="formulario__fila">
                <div class="campo"><label for="e-transportadora">Transportadora</label><input id="e-transportadora" name="transportadora" [(ngModel)]="transportadora" maxlength="60" required /></div>
                <div class="campo"><label for="e-guia">Número de guía</label><input id="e-guia" name="guia" [(ngModel)]="guia" maxlength="60" required /></div>
              </div>
              <div class="acciones"><button class="boton boton--primario" type="submit" [disabled]="!transportadora.trim() || !guia.trim() || ocupado()">Registrar envío y marcar enviado</button></div>
            </form>
          }
          @if (d.siguientes.length) {
            <div class="campo"><label for="nota">Nota (obligatoria para cancelar un pedido pagado)</label><input id="nota" [(ngModel)]="nota" maxlength="255" /></div>
            <div class="acciones">
              @for (e of d.siguientes; track e) {
                @if (e !== 'ENVIADO') {
                  <button type="button" [class]="'boton ' + (e === 'CANCELADO' ? 'boton--secundario' : 'boton--primario')"
                          (click)="avanzar(e)" [disabled]="ocupado()">{{ e === 'CANCELADO' ? 'Cancelar pedido' : 'Marcar ' + nombres[e].toLowerCase() }}</button>
                }
              }
            </div>
          }
        </section>

        <div class="columnas">
          <section class="panel-seccion">
            <h2>Cliente y envío</h2>
            <p>{{ d.cliente.nombre }} · {{ d.cliente.correo }} @if (d.cliente.telefono) { · {{ d.cliente.telefono }} }</p>
            <p>{{ d.pedido.direccion.destinatario }} · {{ d.pedido.direccion.telefono }}<br />{{ d.pedido.direccion.direccion }}, {{ d.pedido.direccion.ciudad }}, {{ d.pedido.direccion.departamento }}</p>
            @if (d.pedido.envio; as en) { <p>{{ en.transportadora }} · guía {{ en.numeroGuia }}@if (en.fechaEntrega) { · entregado {{ en.fechaEntrega | date: 'short' }} }</p> }
          </section>
          <section class="panel-seccion">
            <h2>Pagos</h2>
            <ul>@for (p of d.pagos; track p.referencia) { <li>{{ p.referencia }} · {{ p.estado }} @if (p.metodo) { ({{ p.metodo }}) } · {{ p.monto | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</li> }</ul>
          </section>
        </div>

        <section class="panel-seccion tabla-contenedor">
          <h2>Piezas</h2>
          <table class="tabla">
            <thead><tr><th scope="col">Pieza</th><th scope="col">Cant.</th><th scope="col">Precio</th><th scope="col">Desc.</th><th scope="col">Subtotal</th></tr></thead>
            <tbody>@for (l of d.pedido.lineas; track $index) {
              <tr><td>{{ l.nombre }}@if (l.talla) { · {{ l.talla }} }</td><td>{{ l.cantidad }}</td>
                <td>{{ l.precioUnitario | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</td><td>{{ l.descuento | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</td>
                <td>{{ l.subtotal | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</td></tr>
            }</tbody>
          </table>
        </section>
        <section class="panel-seccion">
          <h2>Historial</h2>
          <ol>@for (h of d.pedido.historial; track $index) { <li>{{ nombres[h.estado] }} · {{ h.fecha | date: 'medium' }}@if (h.nota) { — {{ h.nota }} }</li> }</ol>
        </section>
      }
    </section>
  `,
  styles: `.columnas { display: grid; gap: 1.5rem; margin-top: 1.5rem; } @media (min-width: 60rem) { .columnas { grid-template-columns: 1fr 1fr; } .columnas .panel-seccion + .panel-seccion { margin-top: 0; } }`,
})
export class PedidoAdmin implements OnInit {
  readonly numero = input.required<string>();
  private readonly operacion = inject(OperacionAdminService);
  protected readonly nombres = NOMBRES_ESTADO_PEDIDO;
  protected readonly detalle = signal<PedidoAdminDetalle | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly ocupado = signal(false);
  protected nota = '';
  protected transportadora = '';
  protected guia = '';

  ngOnInit(): void {
    this.operacion.pedido(this.numero()).subscribe({ next: (d) => this.detalle.set(d), error: (e: unknown) => this.error.set(erroresDe(e).general) });
  }

  protected avanzar(estado: EstadoPedido): void {
    if (estado === 'CANCELADO' && !confirm('¿Cancelar este pedido? Las unidades vuelven al inventario.')) return;
    this.ejecutar(this.operacion.cambiarEstado(this.numero(), estado, this.nota.trim() || null));
  }

  protected despachar(): void {
    this.ejecutar(this.operacion.registrarEnvio(this.numero(), this.transportadora.trim(), this.guia.trim()));
  }

  private ejecutar(peticion: ReturnType<OperacionAdminService['pedido']>): void {
    this.error.set(null);
    this.ocupado.set(true);
    peticion.subscribe({
      next: (d) => {
        this.ocupado.set(false);
        this.nota = '';
        this.detalle.set(d);
      },
      error: (e: unknown) => {
        this.ocupado.set(false);
        this.error.set(erroresDe(e).general);
      },
    });
  }
}
