import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-admin-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="admin">
      <nav class="admin__nav" aria-label="Panel de administración">
        <a routerLink="pedidos" routerLinkActive="activo">Pedidos</a>
        <a routerLink="productos" routerLinkActive="activo">Productos</a>
        <a routerLink="inventario" routerLinkActive="activo">Inventario</a>
        <a routerLink="categorias" routerLinkActive="activo">Categorías</a>
        <a routerLink="materiales" routerLinkActive="activo">Materiales</a>
        <a routerLink="promociones" routerLinkActive="activo">Cupones</a>
        <a routerLink="resenas" routerLinkActive="activo">Reseñas</a>
        <a routerLink="reportes" routerLinkActive="activo">Reportes</a>
        <a routerLink="auditoria" routerLinkActive="activo">Auditoría</a>
      </nav>
      <div class="admin__contenido"><router-outlet /></div>
    </div>
  `,
  styles: `
    .admin { display: grid; grid-template-columns: 1fr; min-height: calc(100dvh - 70px); }
    .admin__nav {
      display: flex; gap: 0.25rem; overflow-x: auto; padding: 0.5rem var(--gutter);
      border-bottom: 1px solid var(--regla-suave);
    }
    .admin__nav a { padding: 0.55rem 0.85rem; text-decoration: none; color: var(--tinta-2); white-space: nowrap; }
    .admin__nav a.activo { color: var(--tinta); box-shadow: inset 0 -2px 0 var(--acento); }
    @media (min-width: 60rem) {
      .admin { grid-template-columns: 13rem 1fr; }
      .admin__nav { flex-direction: column; border-bottom: 0; border-right: 1px solid var(--regla-suave); padding: 1.5rem 0.75rem; }
      .admin__nav a.activo { box-shadow: inset 2px 0 0 var(--acento); }
    }
  `,
})
export class AdminLayout {}
