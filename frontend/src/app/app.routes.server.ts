import { RenderMode, ServerRoute } from '@angular/ssr';

// Plan de desarrollo §11: la tienda pública se renderiza en el servidor en cada petición (SEO);
// la cuenta y el panel dependen de la sesión, que solo existe en el navegador.
export const serverRoutes: ServerRoute[] = [
  { path: 'cuenta/**', renderMode: RenderMode.Client },
  { path: 'cuenta', renderMode: RenderMode.Client },
  { path: 'admin/**', renderMode: RenderMode.Client },
  { path: 'admin', renderMode: RenderMode.Client },
  { path: 'bolsa', renderMode: RenderMode.Client },
  { path: 'checkout', renderMode: RenderMode.Client },
  { path: 'pago/**', renderMode: RenderMode.Client },
  { path: '**', renderMode: RenderMode.Server },
];
