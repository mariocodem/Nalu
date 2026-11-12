import { HttpErrorResponse } from '@angular/common/http';
import { SIN_CONEXION, erroresDe } from './problema';

describe('erroresDe', () => {
  it('reparte los errores de validación por campo', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: { detail: 'Revisa', errores: [{ campo: 'correo', mensaje: 'Ingresa un correo válido' }] },
    });
    expect(erroresDe(error)).toEqual({ general: null, campos: { correo: 'Ingresa un correo válido' } });
  });

  it('usa el detail como mensaje general en 401 y 409', () => {
    for (const status of [401, 409]) {
      const error = new HttpErrorResponse({ status, error: { detail: 'Mensaje del servidor' } });
      expect(erroresDe(error).general).toBe('Mensaje del servidor');
    }
  });

  it('informa la falta de conexión', () => {
    expect(erroresDe(new HttpErrorResponse({ status: 0 })).general).toBe(SIN_CONEXION);
  });
});
