/**
 * Convierte el texto del asistente (markdown ligero: **negrita** y listas con «-», «*», «•» o «1.»)
 * en bloques que la plantilla pinta con nodos de texto. Nunca se inserta HTML: lo que escriba el
 * modelo no puede inyectar marcado.
 */
export interface Segmento { texto: string; negrita: boolean }
export type Bloque =
  | { tipo: 'parrafo'; segmentos: Segmento[] }
  | { tipo: 'lista'; ordenada: boolean; items: Segmento[][] };

const VINETA = /^\s*(?:[-*•]|\d+[.)])\s+/;
const NUMERADA = /^\s*\d+[.)]\s+/;

export function formatear(texto: string): Bloque[] {
  const bloques: Bloque[] = [];
  let parrafo: string[] = [];
  let lista: { ordenada: boolean; items: string[] } | null = null;

  const cerrarParrafo = () => {
    if (parrafo.length) bloques.push({ tipo: 'parrafo', segmentos: segmentos(parrafo.join('\n')) });
    parrafo = [];
  };
  const cerrarLista = () => {
    if (lista) bloques.push({ tipo: 'lista', ordenada: lista.ordenada, items: lista.items.map(segmentos) });
    lista = null;
  };

  for (const linea of texto.replace(/\r\n?/g, '\n').split('\n')) {
    if (!linea.trim()) {
      cerrarParrafo();
      cerrarLista();
    } else if (VINETA.test(linea)) {
      cerrarParrafo();
      const ordenada = NUMERADA.test(linea);
      if (!lista || lista.ordenada !== ordenada) {
        cerrarLista();
        lista = { ordenada, items: [] };
      }
      lista.items.push(linea.replace(VINETA, ''));
    } else {
      cerrarLista();
      parrafo.push(linea.trim().replace(/^#+\s*/, ''));
    }
  }
  cerrarParrafo();
  cerrarLista();
  return bloques;
}

/** Separa los tramos en **negrita**; los asteriscos sueltos que queden se descartan. */
export function segmentos(texto: string): Segmento[] {
  const resultado: Segmento[] = [];
  const partes = texto.split(/\*\*(.+?)\*\*/g);
  partes.forEach((parte, i) => {
    const limpio = i % 2 === 1 ? parte : parte.replace(/\*\*/g, '');
    if (limpio) resultado.push({ texto: limpio, negrita: i % 2 === 1 });
  });
  return resultado;
}
