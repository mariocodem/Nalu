import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { Registro } from './registro';

describe('Registro', () => {
  let http: HttpTestingController;

  function crear() {
    TestBed.configureTestingModule({
      imports: [Registro],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(Registro);
    fixture.detectChanges();
    return fixture;
  }

  function llenar(el: HTMLElement, politica: boolean) {
    const poner = (id: string, valor: string) => {
      const input = el.querySelector<HTMLInputElement>('#' + id)!;
      input.value = valor;
      input.dispatchEvent(new Event('input'));
    };
    poner('nombre', 'Eva');
    poner('apellido', 'Ruiz');
    poner('correo', 'eva@nalu.co');
    poner('telefono', '3001234567');
    poner('contrasena', 'secreto-123');
    if (politica) {
      el.querySelector<HTMLInputElement>('#aceptaPolitica')!.click();
    }
  }

  afterEach(() => http.verify());

  it('no envía nada si falta aceptar la política', () => {
    const fixture = crear();
    llenar(fixture.nativeElement, false);
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    http.expectNone('/api/v1/auth/registro');
    expect(fixture.nativeElement.textContent).toContain('Debes aceptar la política');
  });

  it('un correo ya registrado se muestra junto al campo', () => {
    const fixture = crear();
    llenar(fixture.nativeElement, true);
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    http.expectOne('/api/v1/auth/registro').flush(
      { detail: 'Ya existe una cuenta con ese correo' },
      { status: 409, statusText: 'Conflict' },
    );
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#correo-error').textContent).toContain('Ya existe una cuenta');
  });

  it('al registrarse guarda la sesión y vuelve al inicio', () => {
    const fixture = crear();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    llenar(fixture.nativeElement, true);
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    const peticion = http.expectOne('/api/v1/auth/registro');
    expect(peticion.request.body.aceptaPolitica).toBe(true);
    peticion.flush({
      accessToken: 'jwt',
      expiraEn: '2026-11-09T15:00:00Z',
      usuario: { id: 1, nombre: 'Eva', apellido: 'Ruiz', correo: 'eva@nalu.co', roles: ['CLIENTE'] },
    });
    expect(TestBed.inject(AuthService).autenticado()).toBe(true);
    expect(navegar).toHaveBeenCalledWith('/');
  });
});
