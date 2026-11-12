#!/usr/bin/env node
// Catálogo de demostración para la presentación en clase.
//
//   node --env-file=.env scripts/demo/cargar-demo.mjs [urlApi=http://localhost:8080]
//
// Crea y publica piezas con fotografías reales a través de la API de administración, de modo que
// pasan por las mismas validaciones que un producto creado en el panel. Es idempotente: una pieza
// cuyo SKU ya existe se deja como está.
//
// Las fotos de `scripts/demo/fotos/` son de terceros y solo sirven para uso académico: no se
// versionan (.gitignore) y no deben usarse en producción (ver PRODUCT.md, «Evidence on Hand»).
// Precios, nombres y existencias son datos de ejemplo, no decisiones comerciales.

import { readFile } from 'node:fs/promises';
import { join } from 'node:path';

const base = (process.argv[2] ?? 'http://localhost:8080') + '/api/v1';
const dirFotos = join(import.meta.dirname, 'fotos');
const { NALU_ADMIN_EMAIL: correo, NALU_ADMIN_PASSWORD: contrasena } = process.env;

// Orden de creación: la última pieza es la más reciente y queda como pieza de la sala en el inicio.
const PIEZAS = [
  {
    sku: 'DEMO-CAD-ESPIGA', nombre: 'Cadena Espiga', categoria: 'collares', precioBase: 145000,
    descripcion: 'Cadena plana tipo espiga que se asienta sobre la clavícula como una línea de luz. Pensada para el uso diario.',
    materiales: [['Acero', 'METAL_PRINCIPAL']],
    variantes: [['40 cm', 0, 6], ['45 cm', 10000, 4]],
    fotos: [['cadena-espiga.jpg', 'Cadena espiga dorada puesta sobre el cuello, con blazer negro'],
            ['cadena-espiga-puesta.jpg', 'Cadena espiga dorada vista completa sobre fondo neutro']],
  },
  {
    sku: 'DEMO-ANI-ONDA', nombre: 'Anillos Onda', categoria: 'anillos', precioBase: 98000,
    descripcion: 'Aros finos para combinar entre sí: uno en forma de onda y otro con pequeñas piedras.',
    materiales: [['Plata', 'METAL_PRINCIPAL'], ['Cuarzo', 'PIEDRA']],
    variantes: [['6', 0, 3], ['7', 0, 5], ['8', 0, 2]],
    fotos: [['anillos-onda.jpg', 'Mano con anillos finos dorados combinados, uno en forma de onda']],
  },
  {
    sku: 'DEMO-COL-LARIAT', nombre: 'Collar Lariat Gota', categoria: 'collares', precioBase: 210000,
    descripcion: 'Dos cadenas superpuestas: un dije en forma de gota y una caída en Y que termina en una piedra.',
    materiales: [['Oro', 'METAL_PRINCIPAL'], ['Cuarzo', 'PIEDRA']],
    variantes: [['42 cm', 0, 3], ['45 cm', 15000, 2]],
    fotos: [['collar-lariat.jpg', 'Collar doble dorado con dije de gota y caída en Y sobre blusa blanca'],
            ['conjunto-puesto.jpg', 'Conjunto completo puesto: collares, anillos y brazalete dorados']],
  },
  {
    sku: 'DEMO-BRA-GOTA', nombre: 'Brazalete Abierto Gota', categoria: 'pulseras', precioBase: 175000,
    descripcion: 'Brazalete rígido y abierto con dos piedras talladas en sus extremos, una en gota y otra rectangular.',
    materiales: [['Oro', 'METAL_PRINCIPAL'], ['Cuarzo', 'PIEDRA']],
    variantes: [['S', 0, 2], ['M', 0, 4], ['L', 0, 2]],
    fotos: [['brazalete-gota.jpg', 'Brazalete dorado abierto con dos piedras claras en la muñeca'],
            ['conjunto-puesto.jpg', 'Conjunto completo puesto: collares, anillos y brazalete dorados']],
  },
  {
    sku: 'DEMO-ARG-TRENZA', nombre: 'Argollas Trenzadas', categoria: 'aretes', precioBase: 120000,
    descripcion: 'Argollas gruesas de hilo trenzado con cierre de bisagra. Ligeras para su tamaño.',
    materiales: [['Plata', 'METAL_PRINCIPAL']],
    variantes: [[null, 0, 8]],
    fotos: [['argollas-trenzadas.jpg', 'Par de argollas trenzadas doradas sobre una piedra blanca a la luz del sol'],
            ['argollas-trenzadas-ambiente.jpg', 'Argollas trenzadas doradas con sombras de hojas sobre fondo claro']],
  },
  {
    sku: 'DEMO-PUL-ESLABON', nombre: 'Pulsera Eslabón Rolo', categoria: 'pulseras', precioBase: 160000,
    descripcion: 'Eslabones ovalados de buen calibre con cierre de mosquetón y un pequeño corazón al final.',
    materiales: [['Acero', 'METAL_PRINCIPAL']],
    variantes: [['17 cm', 0, 5], ['19 cm', 0, 6], ['21 cm', 8000, 3]],
    fotos: [['pulsera-eslabon.jpg', 'Pulsera de eslabones dorados sobre una revista abierta'],
            ['pulsera-eslabon-ambiente.jpg', 'Pulsera de eslabones dorados sobre una revista, con luz cálida de fondo']],
  },
  {
    sku: 'DEMO-DIJ-ESMERALDA', nombre: 'Dije Gota de Esmeralda', categoria: 'collares', precioBase: 690000,
    descripcion: 'Una esmeralda ovalada en bisel sobre una cadena fina. Discreta y para todos los días.',
    materiales: [['Oro', 'METAL_PRINCIPAL'], ['Esmeralda', 'PIEDRA']],
    variantes: [['40 cm', 0, 2], ['45 cm', 20000, 2]],
    fotos: [['dije-esmeralda.jpg', 'Cadena fina dorada con una pequeña esmeralda ovalada puesta en el cuello']],
  },
  {
    sku: 'DEMO-ARE-LAGRIMA', nombre: 'Aretes Lágrima de Esmeralda', categoria: 'aretes', precioBase: 1850000,
    descripcion: 'Esmeraldas en talla pera rodeadas por un halo de piedras pequeñas, colgando de un botón en gota.',
    materiales: [['Oro', 'METAL_PRINCIPAL'], ['Esmeralda', 'PIEDRA']],
    variantes: [[null, 0, 1]],
    fotos: [['aretes-lagrima.jpg', 'Aretes colgantes de esmeralda en talla pera con halo, en oro, dentro de su estuche']],
  },
  {
    sku: 'DEMO-ANI-TRILOGIA', nombre: 'Anillo Trilogía Esmeralda', categoria: 'anillos', precioBase: 1290000,
    descripcion: 'Una esmeralda redonda en cuatro garras, acompañada por dos piedras blancas a los lados.',
    materiales: [['Oro', 'METAL_PRINCIPAL'], ['Esmeralda', 'PIEDRA']],
    variantes: [['6', 0, 1], ['7', 0, 2], ['8', 0, 1]],
    fotos: [['anillo-trilogia.jpg', 'Anillo de oro amarillo con esmeralda redonda central y dos piedras blancas laterales']],
  },
];

async function api(token, metodo, ruta, cuerpo) {
  const esForm = cuerpo instanceof FormData;
  const r = await fetch(base + ruta, {
    method: metodo,
    headers: {
      ...(token && { Authorization: `Bearer ${token}` }),
      ...(cuerpo && !esForm && { 'Content-Type': 'application/json' }),
    },
    body: cuerpo && (esForm ? cuerpo : JSON.stringify(cuerpo)),
  });
  const datos = r.headers.get('content-type')?.includes('json') ? await r.json() : null;
  if (!r.ok) throw new Error(`${metodo} ${ruta} → ${r.status} ${datos?.detail ?? JSON.stringify(datos?.errores ?? '')}`);
  return datos;
}

async function main() {
  if (!correo || !contrasena) throw new Error('Faltan NALU_ADMIN_EMAIL y NALU_ADMIN_PASSWORD (usa --env-file=.env)');
  const { accessToken: t } = await api(null, 'POST', '/auth/login', { correo, contrasena });

  const categorias = await api(t, 'GET', '/admin/categorias');
  const idCategoria = (slug) => {
    const c = categorias.find((c) => c.slug === slug);
    if (!c) throw new Error(`No existe la categoría «${slug}»; créala en el panel`);
    return c.id;
  };
  const materiales = await api(null, 'GET', '/materiales');
  const idMaterial = (tipo) => {
    const m = materiales.find((m) => m.tipo === tipo);
    if (!m) throw new Error(`No existe el material «${tipo}»; créalo en el panel`);
    return m.id;
  };

  for (const p of PIEZAS) {
    const existentes = await api(t, 'GET', `/admin/productos?q=${p.sku}&size=5`);
    if (existentes.contenido.some((e) => e.sku === p.sku)) {
      console.log(`= ${p.sku} ya existe`);
      continue;
    }
    const { id } = await api(t, 'POST', '/admin/productos', {
      sku: p.sku, nombre: p.nombre, slug: null, descripcion: p.descripcion,
      idCategoria: idCategoria(p.categoria), precioBase: p.precioBase,
    });
    await api(t, 'PUT', `/admin/productos/${id}/materiales`,
      p.materiales.map(([tipo, rol]) => ({ idMaterial: idMaterial(tipo), rol, pesoGramos: null, quilates: null })));
    for (const [i, [talla, precioAdicional, existenciaInicial]] of p.variantes.entries()) {
      await api(t, 'POST', `/admin/productos/${id}/variantes`,
        { sku: `${p.sku}-${i + 1}`, talla, pesoGramos: null, precioAdicional, existenciaInicial });
    }
    for (const [archivo, textoAlternativo] of p.fotos) {
      const form = new FormData();
      form.append('archivo', new Blob([await readFile(join(dirFotos, archivo))], { type: 'image/jpeg' }), archivo);
      form.append('textoAlternativo', textoAlternativo);
      await api(t, 'POST', `/admin/productos/${id}/imagenes`, form);
    }
    await api(t, 'PATCH', `/admin/productos/${id}/estado`, { estado: 'PUBLICADO' });
    console.log(`+ ${p.sku} publicado (${p.variantes.length} variantes, ${p.fotos.length} fotos)`);
  }
}

main().catch((e) => {
  console.error(e.message);
  process.exit(1);
});
