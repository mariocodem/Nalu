import { destinoSeguro } from './destino-seguro';

describe('destinoSeguro', () => {
  it('acepta rutas internas', () => {
    expect(destinoSeguro('/admin/productos')).toBe('/admin/productos');
  });

  it('rechaza redirecciones a otros sitios', () => {
    for (const malo of ['//malo.com', 'https://malo.com', '/\\malo.com', null, '']) {
      expect(destinoSeguro(malo)).toBe('/');
    }
  });
});
