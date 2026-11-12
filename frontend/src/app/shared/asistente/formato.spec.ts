import { formatear, segmentos } from './formato';

describe('formato del asistente', () => {
  it('separa la negrita sin interpretar HTML', () => {
    expect(segmentos('El **Anillo Trilogía** cuesta **$1.290.000** <b>ya</b>')).toEqual([
      { texto: 'El ', negrita: false },
      { texto: 'Anillo Trilogía', negrita: true },
      { texto: ' cuesta ', negrita: false },
      { texto: '$1.290.000', negrita: true },
      { texto: ' <b>ya</b>', negrita: false },
    ]);
  });

  it('arma párrafos y listas con viñetas o números', () => {
    const bloques = formatear('Te recomiendo:\n\n- **Plata 925**: ligera\n* Oro 18K\n\n1. Mide tu dedo\n2. Elige la talla\nListo.');
    expect(bloques.map((b) => b.tipo)).toEqual(['parrafo', 'lista', 'lista', 'parrafo']);
    expect(bloques[1]).toMatchObject({ ordenada: false, items: [[{ texto: 'Plata 925', negrita: true }, { texto: ': ligera', negrita: false }], [{ texto: 'Oro 18K', negrita: false }]] });
    expect(bloques[2]).toMatchObject({ ordenada: true });
  });

  it('descarta encabezados y asteriscos sueltos', () => {
    expect(formatear('## Opciones\nun **anillo')).toEqual([
      { tipo: 'parrafo', segmentos: [{ texto: 'Opciones\nun anillo', negrita: false }] },
    ]);
  });
});
