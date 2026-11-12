#!/usr/bin/env node
// Prueba de carga de Nalu (RNF06): N usuarios concurrentes recorren la tienda y usan la bolsa.
// Criterio: el 95 % de las respuestas de la API en 2 s o menos, sin errores.
//
//   node scripts/prueba-carga.mjs [urlApi=http://localhost:8080] [usuarios=50] [segundos=60]
//
// Cada usuario virtual repite: catálogo → filtro → categorías → ficha → ver su bolsa (añade una pieza
// la primera vez), con una pausa corta entre vueltas. Usa solo fetch (Node 18+), sin dependencias.

const [base = 'http://localhost:8080', usuarios = '50', segundos = '60'] = process.argv.slice(2);
const fin = Date.now() + Number(segundos) * 1000;
const medidas = new Map(); // operación → { tiempos: [], errores: 0 }

async function medir(operacion, url, opciones = {}) {
  const inicio = performance.now();
  let ok = false;
  let cuerpo = null;
  try {
    const r = await fetch(base + url, opciones);
    cuerpo = r.headers.get('content-type')?.includes('json') ? await r.json() : await r.text();
    ok = r.ok;
  } catch {
    ok = false;
  }
  const m = medidas.get(operacion) ?? { tiempos: [], errores: 0 };
  m.tiempos.push(performance.now() - inicio);
  if (!ok) m.errores++;
  medidas.set(operacion, m);
  return cuerpo;
}

const pausa = (ms) => new Promise((r) => setTimeout(r, ms));

async function usuario() {
  let token = null;
  while (Date.now() < fin) {
    const pagina = await medir('catalogo', '/api/v1/productos?size=24');
    await medir('catalogo-filtrado', '/api/v1/productos?disponible=true&sort=precioDesde,asc');
    await medir('categorias', '/api/v1/categorias');
    const slug = pagina?.contenido?.[Math.floor(Math.random() * (pagina.contenido.length || 1))]?.slug;
    if (slug) {
      const ficha = await medir('ficha', `/api/v1/productos/${slug}`);
      const variante = ficha?.variantes?.find((v) => v.disponible);
      if (variante && !token) { // una vez por usuario: la bolsa no puede superar las existencias
        const headers = { 'Content-Type': 'application/json', ...(token ? { 'X-Cart-Token': token } : {}) };
        const bolsa = await medir('bolsa-agregar', '/api/v1/carrito/items', {
          method: 'POST', headers, body: JSON.stringify({ idVariante: variante.id, cantidad: 1 }),
        });
        token = bolsa?.token ?? token;
      }
      if (token) await medir('bolsa-ver', '/api/v1/carrito', { headers: { 'X-Cart-Token': token } });
    }
    await pausa(200 + Math.random() * 300);
  }
}

const percentil = (ordenados, p) => ordenados[Math.min(ordenados.length - 1, Math.ceil((p / 100) * ordenados.length) - 1)];
const inicio = Date.now();
await Promise.all(Array.from({ length: Number(usuarios) }, usuario));
const duracion = (Date.now() - inicio) / 1000;

let total = 0;
let errores = 0;
const todos = [];
console.log(`\n${usuarios} usuarios concurrentes durante ${duracion.toFixed(0)} s contra ${base}\n`);
console.log('operación            peticiones  errores   p50 ms   p95 ms   p99 ms   máx ms');
for (const [op, m] of [...medidas].sort()) {
  const t = [...m.tiempos].sort((a, b) => a - b);
  total += t.length;
  errores += m.errores;
  todos.push(...t);
  console.log(`${op.padEnd(20)} ${String(t.length).padStart(10)} ${String(m.errores).padStart(8)} ${percentil(t, 50).toFixed(0).padStart(8)} ${percentil(t, 95).toFixed(0).padStart(8)} ${percentil(t, 99).toFixed(0).padStart(8)} ${t.at(-1).toFixed(0).padStart(8)}`);
}
todos.sort((a, b) => a - b);
const p95 = percentil(todos, 95);
console.log(`\nTotal: ${total} peticiones (${(total / duracion).toFixed(0)}/s), ${errores} errores, p95 global ${p95.toFixed(0)} ms`);
const cumple = p95 <= 2000 && errores === 0;
console.log(cumple ? 'RNF06 CUMPLE: el 95 % de las respuestas en ≤ 2 s, sin errores.' : 'RNF06 NO CUMPLE.');
process.exit(cumple ? 0 : 1);
