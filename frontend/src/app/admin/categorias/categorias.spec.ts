import { aplanar } from './categorias';

describe('aplanar', () => {
  it('recorre el árbol en profundidad con su nivel', () => {
    const hoja = { id: 2, nombre: 'Plata', slug: 'plata', descripcion: null, activa: true, idPadre: 1, hijos: [] };
    const raiz = { id: 1, nombre: 'Anillos', slug: 'anillos', descripcion: null, activa: true, idPadre: null, hijos: [hoja] };
    expect(aplanar([raiz], 0).map((f) => [f.categoria.id, f.nivel])).toEqual([[1, 0], [2, 1]]);
  });
});
