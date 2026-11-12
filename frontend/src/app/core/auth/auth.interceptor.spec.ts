import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { Sesion, UsuarioSesion } from './sesion.model';

describe('authInterceptor', () => {
  const USUARIO: UsuarioSesion = {
    id: 7,
    nombre: 'Ana',
    apellido: 'Pérez',
    correo: 'ana@nalu.local',
    roles: ['CLIENTE'],
  };

  let servicio: AuthService;
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let router: Router;

  function sesion(accessToken: string): Sesion {
    return {
      accessToken,
      expiraEn: '2026-11-09T15:15:00Z',
      usuario: USUARIO,
    };
  }

  function iniciarSesion(accessToken = 'jwt-viejo'): void {
    servicio.login('ana@nalu.local', 'secreta').subscribe();
    httpMock.expectOne('/api/v1/auth/login').flush(sesion(accessToken));
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });

    servicio = TestBed.inject(AuthService);
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => httpMock.verify());

  it('añade Bearer a las llamadas a /api/', () => {
    iniciarSesion('jwt-1');

    http.get('/api/v1/productos').subscribe();

    const peticion = httpMock.expectOne('/api/v1/productos');
    expect(peticion.request.headers.get('Authorization')).toBe('Bearer jwt-1');
    peticion.flush([]);
  });

  it('no añade Bearer al login ni a URLs externas', () => {
    iniciarSesion('jwt-1');

    http.post('/api/v1/auth/login', { correo: 'x', contrasena: 'y' }).subscribe();
    const login = httpMock.expectOne('/api/v1/auth/login');
    expect(login.request.headers.has('Authorization')).toBe(false);
    login.flush(sesion('jwt-2'));

    http.get('https://ejemplo.com/api/v1/productos').subscribe();
    const externa = httpMock.expectOne('https://ejemplo.com/api/v1/productos');
    expect(externa.request.headers.has('Authorization')).toBe(false);
    externa.flush([]);
  });

  it('ante un 401 hace un refresh y reintenta una vez con el token nuevo', () => {
    iniciarSesion('jwt-viejo');
    let respuesta: unknown;

    http.get('/api/v1/productos').subscribe((cuerpo) => {
      respuesta = cuerpo;
    });

    const original = httpMock.expectOne('/api/v1/productos');
    expect(original.request.headers.get('Authorization')).toBe('Bearer jwt-viejo');
    original.flush({}, { status: 401, statusText: 'No autorizado' });

    const refresco = httpMock.expectOne('/api/v1/auth/refresh');
    expect(refresco.request.withCredentials).toBe(true);
    expect(refresco.request.headers.has('Authorization')).toBe(false);
    refresco.flush(sesion('jwt-nuevo'));

    const reintento = httpMock.expectOne('/api/v1/productos');
    expect(reintento.request.headers.get('Authorization')).toBe('Bearer jwt-nuevo');
    reintento.flush([{ id: 1 }]);

    expect(respuesta).toEqual([{ id: 1 }]);
    expect(servicio.accessToken()).toBe('jwt-nuevo');
  });

  it('dos peticiones con 401 simultáneas comparten un solo refresh', () => {
    iniciarSesion('jwt-viejo');

    http.get('/api/v1/productos').subscribe({ error: () => undefined });
    http.get('/api/v1/pedidos').subscribe({ error: () => undefined });

    const productos = httpMock.expectOne('/api/v1/productos');
    const pedidos = httpMock.expectOne('/api/v1/pedidos');

    productos.flush({}, { status: 401, statusText: 'No autorizado' });
    pedidos.flush({}, { status: 401, statusText: 'No autorizado' });

    const refresco = httpMock.expectOne('/api/v1/auth/refresh');
    refresco.flush(sesion('jwt-nuevo'));

    const reintentoProductos = httpMock.expectOne('/api/v1/productos');
    const reintentoPedidos = httpMock.expectOne('/api/v1/pedidos');

    expect(reintentoProductos.request.headers.get('Authorization')).toBe('Bearer jwt-nuevo');
    expect(reintentoPedidos.request.headers.get('Authorization')).toBe('Bearer jwt-nuevo');

    reintentoProductos.flush([]);
    reintentoPedidos.flush([]);
  });

  it('si el refresh falla limpia la sesión, navega al login y propaga el error original', () => {
    iniciarSesion('jwt-viejo');
    const navegar = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    let errorRecibido: HttpErrorResponse | undefined;

    http.get('/api/v1/productos').subscribe({
      error: (error: HttpErrorResponse) => {
        errorRecibido = error;
      },
    });

    const original = httpMock.expectOne('/api/v1/productos');
    original.flush({}, { status: 401, statusText: 'No autorizado' });

    const refresco = httpMock.expectOne('/api/v1/auth/refresh');
    refresco.flush({}, { status: 401, statusText: 'No autorizado' });

    expect(servicio.autenticado()).toBe(false);
    expect(servicio.usuario()).toBeNull();
    expect(navegar).toHaveBeenCalledWith('/cuenta/login');
    expect(errorRecibido?.status).toBe(401);
    expect(errorRecibido?.url).toBe('/api/v1/productos');
  });
  it('si el reintento falla por otro motivo, conserva la sesión y propaga ese error', () => {
    iniciarSesion('jwt-viejo');
    const navegar = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    let errorRecibido: HttpErrorResponse | undefined;

    http.get('/api/v1/productos').subscribe({
      error: (error: HttpErrorResponse) => {
        errorRecibido = error;
      },
    });

    httpMock.expectOne('/api/v1/productos').flush({}, { status: 401, statusText: 'No autorizado' });
    httpMock.expectOne('/api/v1/auth/refresh').flush(sesion('jwt-nuevo'));
    httpMock
      .expectOne('/api/v1/productos')
      .flush({}, { status: 500, statusText: 'Error interno' });

    expect(servicio.autenticado()).toBe(true);
    expect(servicio.accessToken()).toBe('jwt-nuevo');
    expect(navegar).not.toHaveBeenCalled();
    expect(errorRecibido?.status).toBe(500);
  });
});
