import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { AuthService } from './auth.service';
import { rolGuard } from './rol.guard';
import { Rol, UsuarioSesion } from './sesion.model';

describe('rolGuard', () => {
  let autenticado = false;
  let usuarioActual: UsuarioSesion | null = null;
  let router: Router;

  const authFalso = {
    autenticado: () => autenticado,
    usuario: () => usuarioActual,
  } as unknown as AuthService;

  beforeEach(() => {
    autenticado = false;
    usuarioActual = null;

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authFalso },
      ],
    });

    router = TestBed.inject(Router);
  });

  function ejecutarGuard(roles: Rol[], url: string): boolean | UrlTree {
    return TestBed.runInInjectionContext(() =>
      rolGuard(...roles)(
        {} as ActivatedRouteSnapshot,
        { url } as RouterStateSnapshot,
      ),
    ) as boolean | UrlTree;
  }

  it('sin sesión redirige a /cuenta/login guardando el destino', () => {
    const resultado = ejecutarGuard(['ADMINISTRADOR'], '/admin/productos');

    expect(resultado).toBeInstanceOf(UrlTree);
    const arbol = resultado as UrlTree;
    expect(router.serializeUrl(arbol)).toContain('/cuenta/login');
    expect(arbol.queryParams['redirect']).toBe('/admin/productos');
  });

  it('un CLIENTE en una ruta de ADMINISTRADOR vuelve a la portada', () => {
    autenticado = true;
    usuarioActual = {
      id: 7,
      nombre: 'Ana',
      apellido: 'Pérez',
      correo: 'ana@nalu.local',
      roles: ['CLIENTE'],
    };

    const resultado = ejecutarGuard(['ADMINISTRADOR'], '/admin/productos');

    expect(resultado).toBeInstanceOf(UrlTree);
    expect(router.serializeUrl(resultado as UrlTree)).toBe('/');
  });

  it('un ADMINISTRADOR puede entrar', () => {
    autenticado = true;
    usuarioActual = {
      id: 1,
      nombre: 'Mario',
      apellido: 'Admin',
      correo: 'admin@nalu.local',
      roles: ['ADMINISTRADOR'],
    };

    expect(ejecutarGuard(['ADMINISTRADOR'], '/admin/productos')).toBe(true);
  });
});
