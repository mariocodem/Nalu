import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';
import { Sesion, UsuarioSesion } from './sesion.model';

describe('AuthService', () => {
  const USUARIO: UsuarioSesion = {
    id: 7,
    nombre: 'Ana',
    apellido: 'Pérez',
    correo: 'ana@nalu.local',
    roles: ['CLIENTE'],
  };

  const SESION: Sesion = {
    accessToken: 'jwt-1',
    expiraEn: '2026-11-09T15:15:00Z',
    usuario: USUARIO,
  };

  let servicio: AuthService;
  let httpMock: HttpTestingController;

  function configurar(plataforma: 'browser' | 'server'): void {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: PLATFORM_ID, useValue: plataforma },
      ],
    });
    servicio = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  }

  beforeEach(() => configurar('browser'));

  afterEach(() => httpMock.verify());

  it('login guarda token y usuario y envía correo y contraseña', () => {
    let usuarioRecibido: UsuarioSesion | undefined;

    servicio.login('ana@nalu.local', 'secreta').subscribe((usuario) => {
      usuarioRecibido = usuario;
    });

    const peticion = httpMock.expectOne('/api/v1/auth/login');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({
      correo: 'ana@nalu.local',
      contrasena: 'secreta',
    });
    expect(peticion.request.withCredentials).toBe(false);

    peticion.flush(SESION);

    expect(usuarioRecibido).toEqual(USUARIO);
    expect(servicio.accessToken()).toBe('jwt-1');
    expect(servicio.usuario()).toEqual(USUARIO);
    expect(servicio.autenticado()).toBe(true);
  });

  it('dos refrescar() concurrentes comparten una sola petición', () => {
    const sesiones: Sesion[] = [];

    servicio.refrescar().subscribe((sesion) => sesiones.push(sesion));
    servicio.refrescar().subscribe((sesion) => sesiones.push(sesion));

    const peticiones = httpMock.match('/api/v1/auth/refresh');
    expect(peticiones).toHaveLength(1);
    expect(peticiones[0].request.withCredentials).toBe(true);

    peticiones[0].flush(SESION);

    expect(sesiones).toHaveLength(2);
    expect(servicio.accessToken()).toBe('jwt-1');
  });

  it('logout usa withCredentials y limpia la sesión aunque falle', () => {
    servicio.login('ana@nalu.local', 'secreta').subscribe();
    httpMock.expectOne('/api/v1/auth/login').flush(SESION);

    expect(servicio.autenticado()).toBe(true);

    servicio.logout().subscribe();

    const peticion = httpMock.expectOne('/api/v1/auth/logout');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.withCredentials).toBe(true);

    peticion.flush({}, { status: 500, statusText: 'Error del servidor' });

    expect(servicio.accessToken()).toBeNull();
    expect(servicio.usuario()).toBeNull();
    expect(servicio.autenticado()).toBe(false);
  });

  it('en el servidor restaurarSesion no hace ninguna petición', async () => {
    configurar('server');

    await servicio.restaurarSesion();

    httpMock.verify();
    expect(servicio.autenticado()).toBe(false);
    expect(servicio.usuario()).toBeNull();
  });

  it('restaurarSesion con 401 deja la sesión vacía y no lanza', async () => {
    const restauracion = servicio.restaurarSesion();

    const peticion = httpMock.expectOne('/api/v1/auth/refresh');
    expect(peticion.request.withCredentials).toBe(true);
    peticion.flush({}, { status: 401, statusText: 'No autorizado' });

    await expect(restauracion).resolves.toBeUndefined();
    expect(servicio.autenticado()).toBe(false);
    expect(servicio.accessToken()).toBeNull();
    expect(servicio.usuario()).toBeNull();
  });
  it('con Web Locks el refresh se hace dentro del bloqueo compartido entre pestañas', async () => {
    const solicitudes: string[] = [];
    const locksFalso = {
      request: (nombre: string, tarea: () => Promise<unknown>) => {
        solicitudes.push(nombre);
        return tarea();
      },
    };
    Object.defineProperty(navigator, 'locks', { value: locksFalso, configurable: true });
    try {
      const resultado = firstValueFrom(servicio.refrescar());

      httpMock.expectOne('/api/v1/auth/refresh').flush(SESION);

      await expect(resultado).resolves.toEqual(SESION);
      expect(solicitudes).toEqual([AuthService.BLOQUEO_REFRESH]);
      expect(servicio.accessToken()).toBe('jwt-1');
    } finally {
      Reflect.deleteProperty(navigator, 'locks');
    }
  });
});
