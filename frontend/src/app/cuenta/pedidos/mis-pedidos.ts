import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { NOMBRES_ESTADO_PEDIDO, PedidosService } from '../../core/api/pedidos.service';

@Component({
  selector: 'app-mis-pedidos',
  imports: [RouterLink, CurrencyPipe, DatePipe],
  template: `
    <section class="pagina">
      <h1>Mis pedidos</h1>
      @if (pagina(); as p) {
        @if (p.contenido.length === 0) { <p class="nota">Todavía no tienes pedidos. <a routerLink="/catalogo">Ver piezas</a></p> }
        <div class="tabla-contenedor">
          <table class="tabla">
            <thead><tr><th scope="col">Pedido</th><th scope="col">Fecha</th><th scope="col">Estado</th><th scope="col">Total</th></tr></thead>
            <tbody>
              @for (r of p.contenido; track r.numero) {
                <tr>
                  <td><a [routerLink]="[r.numero]">{{ r.numero }}</a></td>
                  <td>{{ r.fecha | date: 'mediumDate' }}</td>
                  <td>{{ estados[r.estado] }}</td>
                  <td>{{ r.total | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `,
})
export class MisPedidos {
  protected readonly estados = NOMBRES_ESTADO_PEDIDO;
  protected readonly pagina = toSignal(inject(PedidosService).misPedidos());
}
