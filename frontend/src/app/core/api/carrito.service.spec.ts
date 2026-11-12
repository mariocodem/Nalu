import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { CarritoService } from './carrito.service';

describe('CarritoService', () => {
  let http: HttpTestingController;
  let servicio: CarritoService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: PLATFORM_ID, useValue: 'browser' }],
    });
    http = TestBed.inject(HttpTestingController);
    servicio = TestBed.inject(CarritoService);
    TestBed.tick(); // el efecto inicial carga la bolsa del visitante
    http.expectOne('/api/v1/carrito').flush({ token: null, items: [], cantidadTotal: 0, subtotal: 0 });
  });

  afterEach(() => http.verify());

  it('guarda el token del visitante y lo envía en las siguientes llamadas', () => {
    servicio.agregar(7, 1).subscribe();
    const primera = http.expectOne('/api/v1/carrito/items');
    expect(primera.request.headers.has('X-Cart-Token')).toBe(false);
    primera.flush({ token: 'tok-123', items: [], cantidadTotal: 1, subtotal: 100 });

    expect(servicio.cantidad()).toBe(1);
    servicio.recargar().subscribe();
    const segunda = http.expectOne('/api/v1/carrito');
    expect(segunda.request.headers.get('X-Cart-Token')).toBe('tok-123');
    segunda.flush({ token: 'tok-123', items: [], cantidadTotal: 1, subtotal: 100 });
  });
});
