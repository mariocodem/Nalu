import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Pagina } from '../../core/api/modelos';

interface Registro {
  id: number; fecha: string; usuario: string; accion: string; recurso: string | null; idRecurso: string | null; detalle: string | null;
}

/** RNF15: quién hizo qué y cuándo en el panel, y los cambios de precio. */
@Component({
  selector: 'app-auditoria',
  imports: [DatePipe],
  template: `
    <section class="pagina">
      <h1>Auditoría</h1>
      <p class="nota">Acciones del panel y cambios de precio, del más reciente al más antiguo. Los cambios de estado de pedidos y de inventario tienen además su propio historial.</p>
      <div class="tabla-contenedor panel-seccion">
        <table class="tabla">
          <thead><tr><th scope="col">Fecha</th><th scope="col">Usuario</th><th scope="col">Acción</th><th scope="col">Detalle</th></tr></thead>
          <tbody>@for (r of pagina()?.contenido ?? []; track r.id) {
            <tr><td>{{ r.fecha | date: 'short' }}</td><td>{{ r.usuario }}</td><td><code>{{ r.accion }}</code></td><td>{{ r.detalle }}</td></tr>
          } @empty { <tr><td colspan="4" class="nota">Sin registros.</td></tr> }</tbody>
        </table>
      </div>
      @if ((pagina()?.totalPaginas ?? 0) > 1) {
        <nav class="acciones" aria-label="Paginación">
          <button class="boton boton--secundario boton--pequeno" type="button" [disabled]="numero() === 0" (click)="cargar(numero() - 1)">Anterior</button>
          <span>Página {{ numero() + 1 }} de {{ pagina()?.totalPaginas }}</span>
          <button class="boton boton--secundario boton--pequeno" type="button" [disabled]="numero() + 1 >= (pagina()?.totalPaginas ?? 0)" (click)="cargar(numero() + 1)">Siguiente</button>
        </nav>
      }
    </section>
  `,
})
export class Auditoria implements OnInit {
  private readonly http = inject(HttpClient);
  protected readonly pagina = signal<Pagina<Registro> | null>(null);
  protected readonly numero = signal(0);

  ngOnInit(): void {
    this.cargar(0);
  }

  protected cargar(pagina: number): void {
    this.numero.set(pagina);
    this.http.get<Pagina<Registro>>('/api/v1/admin/auditoria', { params: { page: pagina } }).subscribe((p) => this.pagina.set(p));
  }
}
