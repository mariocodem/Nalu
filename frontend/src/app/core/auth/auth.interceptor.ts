import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/** Rutas de autenticación que nunca deben llevar Authorization. */
const RUTAS_SIN_TOKEN = [
  '/api/v1/auth/login',
  '/api/v1/auth/refresh',
  '/api/v1/auth/logout',
];

/**
 * Añade el access token a las llamadas a la API y gestiona la renovación ante un 401.
 *
 * <p>Solo las URL que empiezan por {@code /api/} reciben el header, y nunca las rutas
 * de login, refresh o logout. Ante un 401 de una petición que llevaba token se hace un
 * único refresh compartido y se reintenta una sola vez. Si el refresh falla, se limpia
 * la sesión, se navega al login y se propaga el error original.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const esApi = req.url.startsWith('/api/');
  const esRutaSinToken = RUTAS_SIN_TOKEN.some((ruta) => req.url.startsWith(ruta));
  const token = authService.accessToken();
  const llevaToken = esApi && !esRutaSinToken && token !== null;

  const peticion = llevaToken
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(peticion).pipe(
    catchError((error: unknown) => {
      const es401 = error instanceof HttpErrorResponse && error.status === 401;

      if (!es401 || !llevaToken) {
        return throwError(() => error);
      }

      return authService.refrescar().pipe(
        // Solo un fallo del refresh cierra la sesión. Un error del reintento (p. ej. un 500)
        // se propaga sin tocar la sesión, por eso este catchError va antes del switchMap.
        catchError(() => {
          authService.limpiarSesion();
          void router.navigateByUrl('/cuenta/login');
          return throwError(() => error);
        }),
        switchMap((sesion) =>
          next(
            req.clone({
              setHeaders: { Authorization: `Bearer ${sesion.accessToken}` },
            }),
          ),
        ),
      );
    }),
  );
};
