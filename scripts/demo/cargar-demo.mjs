#!/usr/bin/env node
// Catálogo de demostración para la presentación en clase.
//
//   node --env-file=.env scripts/demo/cargar-demo.mjs [urlApi=http://localhost:8080] [--actualizar]
//
// Crea y publica piezas con fotografías reales a través de la API de administración, de modo que
// pasan por las mismas validaciones que un producto creado en el panel. Es idempotente: una pieza
// cuyo SKU ya existe se deja como está. Con --actualizar, a las piezas existentes se les sincronizan
// el nombre, la descripción y las fotos con este archivo; precio, categoría, materiales, variantes y
// existencias no se tocan.
//
// Fotos de `scripts/demo/fotos/` (no se versionan, ver .gitignore):
// - Las que aparecen en FUENTES son de Unsplash o Pexels, cuyas licencias permiten el uso gratuito
//   (https://unsplash.com/license, https://www.pexels.com/license/). Si faltan, se descargan solas.
//   Créditos en scripts/demo/CREDITOS-FOTOS.md.
// - Las demás son de terceros, solo para uso académico, y hay que copiarlas a mano.
// Ninguna es una pieza real de Nalu: no deben usarse en producción (PRODUCT.md, «Evidence on Hand»).
// Precios, nombres y existencias son datos de ejemplo, no decisiones comerciales.

import { existsSync } from 'node:fs';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { join } from 'node:path';

const argumentos = process.argv.slice(2);
const actualizar = argumentos.includes('--actualizar');
const base = (argumentos.find((a) => !a.startsWith('--')) ?? 'http://localhost:8080') + '/api/v1';
const dirFotos = join(import.meta.dirname, 'fotos');
const { NALU_ADMIN_EMAIL: correo, NALU_ADMIN_PASSWORD: contrasena } = process.env;

/** Origen de las fotos que se pueden descargar: [sitio, id de la foto en ese sitio]. */
const FUENTES = {
  'dije-gota-esmeralda.jpg': ['pexels', '28843751'],
  'aretes-lagrima-esmeralda.jpg': ['unsplash', '1QFJV4mMfU8'],
  'anillo-cruce-esmeralda.jpg': ['unsplash', 'mnMWMNP0soM'],
  'collar-aros-entrelazados.jpg': ['unsplash', '3JkHYiGtT-Y'],
  'collar-medialuna.jpg': ['unsplash', 'Q3dMusCuYKk'],
  'anillo-alianza.jpg': ['unsplash', 'ZYet8yoepik'],
  'anillo-solitario.jpg': ['unsplash', 'NhrcL_C0sFA'],
  'aros-brillo.jpg': ['pexels', '3266700'],
  'pulsera-barbada.jpg': ['pexels', '12026051'],
  'pulsera-barra.jpg': ['pexels', '14509641'],
  'brazalete-brillante.jpg': ['pexels', '34399115'],
  'aretes-flor-esmeralda.jpg': ['unsplash', 'P--msdyjXx4'],
  'anillo-hilera-esmeraldas.jpg': ['unsplash', 'z6ESfwcmQhU'],
  'dije-halo-esmeralda.jpg': ['pexels', '15684101'],
};

const DESCARGA = {
  unsplash: (id) => `https://unsplash.com/photos/${id}/download?force=true&w=1600`,
  pexels: (id) => `https://images.pexels.com/photos/${id}/pexels-photo-${id}.jpeg?auto=compress&cs=tinysrgb&w=1600`,
};

// Orden de creación: las últimas son las más recientes y aparecen primero en las novedades del inicio.
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
    descripcion: 'Una esmeralda en talla pera que cuelga de una cadena fina. Discreta y para todos los días.',
    materiales: [['Oro', 'METAL_PRINCIPAL'], ['Esmeralda', 'PIEDRA']],
    variantes: [['40 cm', 0, 2], ['45 cm', 20000, 2]],
    fotos: [['dije-gota-esmeralda.jpg', 'Cadenas finas doradas sobre tela blanca; en primer plano, una esmeralda en forma de gota']],
  },
  {
    sku: 'DEMO-ARE-LAGRIMA', nombre: 'Aretes Lágrima de Esmeralda', categoria: 'aretes', precioBase: 1850000,
    descripcion: 'Una esmeralda cuadrada rodeada por un halo de piedras pequeñas, un aro de oro y una lágrima de cristal que cuelga al final.',
    materiales: [['Oro', 'METAL_PRINCIPAL'], ['Esmeralda', 'PIEDRA']],
    variantes: [[null, 0, 1]],
    fotos: [['aretes-lagrima-esmeralda.jpg', 'Aretes de oro con esmeralda cuadrada en halo, un aro y una pequeña lágrima colgante, sobre una base roja']],
  },
  {
    sku: 'DEMO-ANI-TRILOGIA', nombre: 'Anillo Cruce de Esmeralda', categoria: 'anillos', precioBase: 1290000,
    descripcion: 'Dos brazos de oro, uno blanco y otro amarillo, que se cruzan para sostener una esmeralda cuadrada.',
    materiales: [['Oro', 'METAL_PRINCIPAL'], ['Esmeralda', 'PIEDRA']],
    variantes: [['6', 0, 1], ['7', 0, 2], ['8', 0, 1]],
    fotos: [['anillo-cruce-esmeralda.jpg', 'Anillo de oro blanco y amarillo con brazos cruzados que sostienen una esmeralda cuadrada']],
  },
  {
    sku: 'DEMO-COL-AROS', nombre: 'Collar Aros Entrelazados', categoria: 'collares', precioBase: 135000,
    descripcion: 'Tres aros finos entrelazados que giran libres sobre una cadena delgada. Un regalo que no falla.',
    materiales: [['Plata', 'METAL_PRINCIPAL']],
    variantes: [['42 cm', 0, 6], ['45 cm', 0, 4]],
    fotos: [['collar-aros-entrelazados.jpg', 'Collar de cadena fina con un dije de aros entrelazados sobre un tejido de punto color óxido']],
  },
  {
    sku: 'DEMO-COL-MEDIALUNA', nombre: 'Collar Medialuna Martillada', categoria: 'collares', precioBase: 280000,
    descripcion: 'Una medialuna dorada con textura martillada a mano que atrapa la luz desde cualquier ángulo.',
    materiales: [['Oro', 'METAL_PRINCIPAL']],
    variantes: [['40 cm', 0, 4], ['45 cm', 10000, 3]],
    fotos: [['collar-medialuna.jpg', 'Dije dorado en forma de medialuna con textura martillada, colgado de una cadena fina sobre fondo gris claro']],
  },
  {
    sku: 'DEMO-ANI-ALIANZA', nombre: 'Alianza Clásica', categoria: 'anillos', precioBase: 460000,
    descripcion: 'Aro liso de media caña, pulido a espejo y cómodo para llevar todos los días. Se vende por unidad.',
    materiales: [['Oro', 'METAL_PRINCIPAL']],
    variantes: [['5', 0, 3], ['6', 0, 4], ['7', 0, 4], ['8', 0, 3], ['9', 0, 2]],
    fotos: [['anillo-alianza.jpg', 'Dos alianzas lisas de oro, una sobre otra, apoyadas en una tela beige']],
  },
  {
    sku: 'DEMO-ANI-SOLITARIO', nombre: 'Anillo Solitario Luz', categoria: 'anillos', precioBase: 540000,
    descripcion: 'Una piedra clara en talla esmeralda sobre cuatro garras, con un brazo fino de pequeñas piedras.',
    materiales: [['Oro', 'METAL_PRINCIPAL'], ['Cuarzo', 'PIEDRA']],
    variantes: [['6', 0, 2], ['7', 0, 3], ['8', 0, 2]],
    fotos: [['anillo-solitario.jpg', 'Anillo solitario de oro con una piedra clara rectangular y el brazo cubierto de piedras pequeñas, sobre fondo blanco']],
  },
  {
    sku: 'DEMO-ARE-AROS', nombre: 'Aros Brillo', categoria: 'aretes', precioBase: 230000,
    descripcion: 'Aros medianos de bandas cruzadas, una pulida y otra con una línea de piedras pequeñas. Cierre de palanca.',
    materiales: [['Oro', 'METAL_PRINCIPAL'], ['Cuarzo', 'PIEDRA']],
    variantes: [[null, 0, 6]],
    fotos: [['aros-brillo.jpg', 'Par de aros dorados de bandas cruzadas con una línea de piedras, reflejados sobre fondo negro']],
  },
  {
    sku: 'DEMO-PUL-BARBADA', nombre: 'Pulsera Barbada', categoria: 'pulseras', precioBase: 189000,
    descripcion: 'Eslabones barbados planos cubiertos de brillo, con un broche de caja firme.',
    materiales: [['Plata', 'METAL_PRINCIPAL'], ['Cuarzo', 'PIEDRA']],
    variantes: [['17 cm', 0, 4], ['19 cm', 0, 4], ['21 cm', 10000, 2]],
    fotos: [['pulsera-barbada.jpg', 'Pulsera plateada de eslabones barbados con brillo, sobre un cilindro turquesa y un disco de madera']],
  },
  {
    sku: 'DEMO-PUL-BARRA', nombre: 'Pulsera Barra Fina', categoria: 'pulseras', precioBase: 165000,
    descripcion: 'Una cadena finísima con una barra pulida al centro. Se combina bien con otras pulseras.',
    materiales: [['Oro', 'METAL_PRINCIPAL']],
    variantes: [['16 cm', 0, 5], ['18 cm', 0, 5]],
    fotos: [['pulsera-barra.jpg', 'Muñeca con una pulsera fina dorada con una barra pulida, junto a otra pulsera con un dije, y suéter crema']],
  },
  {
    sku: 'DEMO-BRA-BRILLANTE', nombre: 'Brazalete Cruce Brillante', categoria: 'pulseras', precioBase: 320000,
    descripcion: 'Brazalete rígido de oro cuyos extremos se cruzan, uno pulido y otro con una línea de piedras.',
    materiales: [['Oro', 'METAL_PRINCIPAL'], ['Cuarzo', 'PIEDRA']],
    variantes: [['S', 0, 2], ['M', 0, 3]],
    fotos: [['brazalete-brillante.jpg', 'Brazalete dorado de extremos cruzados con una línea de piedras, sobre un cojín blanco en su caja']],
  },
  // Las tres últimas piezas con esmeralda son las que muestra la sala Esmeralda del inicio.
  {
    sku: 'DEMO-ARE-FLOR', nombre: 'Aretes Flor de Esmeralda', categoria: 'aretes', precioBase: 760000,
    descripcion: 'Cadenas largas y finas que terminan en una esmeralda y una flor de piedras claras con centro de esmeralda.',
    materiales: [['Oro', 'METAL_PRINCIPAL'], ['Esmeralda', 'PIEDRA']],
    variantes: [[null, 0, 3]],
    fotos: [['aretes-flor-esmeralda.jpg', 'Aretes de cadena fina dorada que terminan en una esmeralda y una flor de piedras claras, sobre satén beige']],
  },
  {
    sku: 'DEMO-ANI-HILERA', nombre: 'Anillo Hilera de Esmeraldas', categoria: 'anillos', precioBase: 1150000,
    descripcion: 'Un aro fino de oro con una hilera de esmeraldas pequeñas engastadas en la mitad superior.',
    materiales: [['Oro', 'METAL_PRINCIPAL'], ['Esmeralda', 'PIEDRA']],
    variantes: [['6', 0, 1], ['7', 0, 2], ['8', 0, 1]],
    fotos: [['anillo-hilera-esmeraldas.jpg', 'Anillo fino de oro con una hilera de esmeraldas dentro de un estuche rosa claro abierto']],
  },
  {
    sku: 'DEMO-DIJ-HALO', nombre: 'Dije Halo de Esmeralda', categoria: 'collares', precioBase: 980000,
    descripcion: 'Una esmeralda ovalada rodeada por un halo de piedras claras, sobre una cadena plateada fina.',
    materiales: [['Plata', 'METAL_PRINCIPAL'], ['Esmeralda', 'PIEDRA']],
    variantes: [['40 cm', 0, 2], ['45 cm', 20000, 1]],
    fotos: [['dije-halo-esmeralda.jpg', 'Dije de esmeralda ovalada con halo de piedras claras y cadena plateada, dentro de un plato de cerámica crema']],
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

/** Descarga la foto desde su fuente si no está en `fotos/`; sin fuente, hay que copiarla a mano. */
async function asegurarFoto(archivo) {
  const ruta = join(dirFotos, archivo);
  if (existsSync(ruta)) return;
  const fuente = FUENTES[archivo];
  if (!fuente) throw new Error(`Falta ${ruta} y no tiene fuente de descarga: cópiala a mano`);
  const [sitio, id] = fuente;
  const r = await fetch(DESCARGA[sitio](id));
  if (!r.ok || !r.headers.get('content-type')?.startsWith('image/')) {
    throw new Error(`No se pudo descargar ${archivo} de ${sitio} (${r.status})`);
  }
  await mkdir(dirFotos, { recursive: true });
  await writeFile(ruta, Buffer.from(await r.arrayBuffer()));
  console.log(`↓ ${archivo} (${sitio})`);
}

async function subirFotos(t, id, fotos) {
  let producto;
  for (const [archivo, textoAlternativo] of fotos) {
    const form = new FormData();
    form.append('archivo', new Blob([await readFile(join(dirFotos, archivo))], { type: 'image/jpeg' }), archivo);
    form.append('textoAlternativo', textoAlternativo);
    producto = await api(t, 'POST', `/admin/productos/${id}/imagenes`, form);
  }
  return producto;
}

/**
 * --actualizar: nombre, descripción y fotos según este archivo. Las fotos nuevas se suben antes de
 * borrar las anteriores, porque un producto publicado no puede quedarse sin imágenes.
 */
async function sincronizar(t, id, p) {
  const actual = await api(t, 'GET', `/admin/productos/${id}`);
  const cambios = [];
  if (actual.nombre !== p.nombre || actual.descripcion !== p.descripcion) {
    await api(t, 'PUT', `/admin/productos/${id}`, {
      sku: actual.sku, nombre: p.nombre, slug: actual.nombre === p.nombre ? actual.slug : null,
      descripcion: p.descripcion, idCategoria: actual.idCategoria, precioBase: actual.precioBase,
    });
    cambios.push('textos');
  }
  const textos = (imagenes) => imagenes.map((i) => i.textoAlternativo).join('\n');
  const anteriores = [...actual.imagenes].sort((a, b) => a.orden - b.orden);
  if (textos(anteriores) !== textos(p.fotos.map(([, textoAlternativo]) => ({ textoAlternativo })))) {
    const conNuevas = await subirFotos(t, id, p.fotos);
    for (const imagen of anteriores) {
      await api(t, 'DELETE', `/admin/productos/${id}/imagenes/${imagen.id}`);
    }
    const primera = conNuevas.imagenes.filter((i) => !anteriores.some((a) => a.id === i.id))
      .sort((a, b) => a.orden - b.orden)[0];
    await api(t, 'PATCH', `/admin/productos/${id}/imagenes/${primera.id}`,
      { textoAlternativo: primera.textoAlternativo, orden: primera.orden, principal: true });
    cambios.push(`${p.fotos.length} fotos`);
  }
  console.log(cambios.length ? `~ ${p.sku} actualizado (${cambios.join(', ')})` : `= ${p.sku} sin cambios`);
}

async function main() {
  if (!correo || !contrasena) throw new Error('Faltan NALU_ADMIN_EMAIL y NALU_ADMIN_PASSWORD (usa --env-file=.env)');
  for (const archivo of new Set(PIEZAS.flatMap((p) => p.fotos.map(([a]) => a)))) {
    await asegurarFoto(archivo);
  }
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
    const existente = existentes.contenido.find((e) => e.sku === p.sku);
    if (existente) {
      if (actualizar) await sincronizar(t, existente.id, p);
      else console.log(`= ${p.sku} ya existe`);
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
    await subirFotos(t, id, p.fotos);
    await api(t, 'PATCH', `/admin/productos/${id}/estado`, { estado: 'PUBLICADO' });
    console.log(`+ ${p.sku} publicado (${p.variantes.length} variantes, ${p.fotos.length} fotos)`);
  }
}

main().catch((e) => {
  console.error(e.message);
  process.exit(1);
});
