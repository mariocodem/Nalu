import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Rol } from './sesion.model';

/**
 * Crea un guard que exige estar autenticado y tener al menos uno de los roles indicados.
 *
 * <p>Si no hay sesión, redirige al login guardando la URL de destino en
 * {@code redirect}. Si hay sesión pero falta el rol, redirige a la portada.
 *
 * <p><strong>Aviso:</strong> esto solo es UX. Quien decide de verdad es el backend;
 * las restricciones del frontend nunca protegen recursos por sí solas.
 *
 * @param roles roles permitidos
 */
export function rolGuard(...roles: Rol[]): CanActivateFn {
  return (_ruta, estado) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.autenticado()) {
      return router.createUrlTree(['/cuenta/login'], {
        queryParams: { redirect: estado.url },
      });
    }

    const usuario = authService.usuario();
    const tieneRol = usuario?.roles.some((rol) => roles.includes(rol)) ?? false;

    if (!tieneRol) {
      return router.createUrlTree(['/']);
    }

    return true;
  };
}
