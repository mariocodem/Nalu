import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PagosService } from './pagos.service';

describe('PagosService', () => {
  let http: HttpTestingController;
  let servicio: PagosService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    servicio = TestBed.inject(PagosService);
  });

  afterEach(() => {
    http.verify();
    vi.restoreAllMocks();
    document.body.querySelectorAll('form').forEach((f) => f.remove());
  });

  it('envía a PayU un formulario POST con un campo oculto por cada dato firmado', () => {
    const enviar = vi.spyOn(HTMLFormElement.prototype, 'submit').mockImplementation(() => undefined);
    const campos = { merchantId: '508029', referenceCode: 'NALU-ABC234-1', amount: '115000', signature: 'f1rma' };

    servicio.irAPasarela({
      urlPago: 'https://sandbox.checkout.payulatam.com/ppp-web-gateway-payu/',
      referencia: 'NALU-ABC234-1',
      montoCentavos: 11500000,
      moneda: 'COP',
      campos,
    });

    const formulario = document.body.querySelector('form')!;
    const entradas = Array.from(formulario.querySelectorAll('input'));
    expect(enviar).toHaveBeenCalledOnce();
    expect(formulario.method).toBe('post');
    expect(formulario.action).toBe('https://sandbox.checkout.payulatam.com/ppp-web-gateway-payu/');
    expect(entradas.every((e) => e.type === 'hidden')).toBe(true);
    expect(Object.fromEntries(entradas.map((e) => [e.name, e.value]))).toEqual(campos);
  });

  it('reenvía al backend la respuesta de PayU tal como llegó', () => {
    const parametros = { referenceCode: 'NALU-ABC234-1', transactionState: '4', TX_VALUE: '115000.00', signature: 'f1rma' };
    let estado: string | undefined;

    servicio.registrarRespuestaPayu(parametros).subscribe((r) => (estado = r.estado));
    const solicitud = http.expectOne('/api/v1/pagos/payu/respuesta');
    expect(solicitud.request.method).toBe('POST');
    expect(solicitud.request.body).toEqual(parametros);
    solicitud.flush({ numero: 'NALU-ABC234', estado: 'PAGADO', estadoPago: 'APROBADO' });

    expect(estado).toBe('PAGADO');
  });
});
