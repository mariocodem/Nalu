import { HttpInterceptorFn } from '@angular/common/http';
import { InjectionToken, inject } from '@angular/core';

/**
 * Origen de la API cuando la app se renderiza en el servidor (SSR): allí no hay navegador que
 * resuelva rutas relativas. En el navegador no se provee y las rutas siguen siendo relativas
 * (mismo origen o proxy de desarrollo).
 */
export const URL_API_SERVIDOR = new InjectionToken<string>('URL_API_SERVIDOR');

export const urlApiInterceptor: HttpInterceptorFn = (req, next) => {
  const base = inject(URL_API_SERVIDOR, { optional: true });
  return base && req.url.startsWith('/api/') ? next(req.clone({ url: base + req.url })) : next(req);
};
