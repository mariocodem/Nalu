import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EstadoResena, OperacionAdminService, ResenaAdmin } from '../../core/api/operacion-admin.service';
import { erroresDe } from '../../core/api/problema';

/** RF07: moderación de reseñas. Solo las aprobadas se publican en la ficha. */
@Component({
  selector: 'app-resenas-admin',
  imports: [FormsModule, RouterLink, DatePipe],
  template: `
    <section class="pagina">
      <h1>Reseñas</h1>
      <div class="campo filtro"><label for="r-estado">Mostrar</label>
        <select id="r-estado" [(ngModel)]="estado" (change)="cargar()">
          <option value="PENDIENTE">Pendientes</option><option value="APROBADA">Aprobadas</option>
          <option value="RECHAZADA">Rechazadas</option><option value="">Todas</option>
        </select></div>
      @if (error(); as e) { <p class="aviso" role="alert">{{ e }}</p> }
      <ul class="resenas">
        @for (r of resenas(); track r.id) {
          <li class="panel-seccion">
            <p><strong>{{ '★'.repeat(r.calificacion) }}{{ '☆'.repeat(5 - r.calificacion) }}</strong> · <a [routerLink]="['/producto', r.slug]">{{ r.producto }}</a></p>
            @if (r.comentario) { <p>{{ r.comentario }}</p> }
            <p class="nota">{{ r.cliente }} · {{ r.correo }} · {{ r.fecha | date: 'medium' }} · {{ r.estado }}</p>
            <div class="acciones">
              @if (r.estado !== 'APROBADA') { <button type="button" class="boton boton--primario boton--pequeno" (click)="moderar(r, 'APROBADA')">Aprobar</button> }
              @if (r.estado !== 'RECHAZADA') { <button type="button" class="boton boton--secundario boton--pequeno" (click)="moderar(r, 'RECHAZADA')">Rechazar</button> }
            </div>
          </li>
        } @empty { <li class="nota">No hay reseñas en esta vista.</li> }
      </ul>
    </section>
  `,
  styles: `.filtro { max-width: 14rem; margin-bottom: 1rem; } .resenas { list-style: none; padding: 0; margin: 0; } .resenas p { margin: 0.25rem 0; }`,
})
export class ResenasAdmin implements OnInit {
  private readonly operacion = inject(OperacionAdminService);
  protected readonly resenas = signal<ResenaAdmin[]>([]);
  protected readonly error = signal<string | null>(null);
  protected estado: EstadoResena | '' = 'PENDIENTE';

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.operacion.resenas(this.estado).subscribe({ next: (p) => this.resenas.set(p.contenido), error: (e: unknown) => this.error.set(erroresDe(e).general) });
  }

  protected moderar(r: ResenaAdmin, estado: EstadoResena): void {
    this.operacion.moderar(r.id, estado).subscribe({ next: () => this.cargar(), error: (e: unknown) => this.error.set(erroresDe(e).general) });
  }
}
