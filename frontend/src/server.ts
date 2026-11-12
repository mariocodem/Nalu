import {
  AngularNodeAppEngine,
  createNodeRequestHandler,
  isMainModule,
  writeResponseToNodeResponse,
} from '@angular/ssr/node';
import express from 'express';
import { join } from 'node:path';

const browserDistFolder = join(import.meta.dirname, '../browser');

const app = express();
const angularApp = new AngularNodeAppEngine();

/**
 * Cabeceras de seguridad de todas las respuestas. La CSP se limita a lo que no interfiere con la
 * hidratación de Angular: nadie puede incrustar la tienda en un iframe (clickjacking), los
 * formularios solo envían a Nalu o a Wompi y no se cargan plugins.
 */
app.use((_req, res, next) => {
  res.set({
    'X-Content-Type-Options': 'nosniff',
    'Referrer-Policy': 'strict-origin-when-cross-origin',
    'X-Frame-Options': 'DENY',
    'Permissions-Policy': 'camera=(), microphone=(), geolocation=(), payment=()',
    'Content-Security-Policy':
      "frame-ancestors 'none'; base-uri 'self'; object-src 'none'; form-action 'self' https://checkout.wompi.co",
  });
  next();
});

/** Origen de la API vista desde este servidor y URL pública de la tienda. */
const urlApi = process.env['NALU_API_URL'] ?? 'http://localhost:8080';
const urlPublica = (process.env['NALU_URL_PUBLICA'] ?? 'http://localhost:4200').replace(/\/$/, '');

/** robots.txt: todo indexable salvo la cuenta y el panel (RNF07). */
app.get('/robots.txt', (_req, res) => {
  res.type('text/plain').send(
    ['User-agent: *', 'Disallow: /cuenta', 'Disallow: /admin', `Sitemap: ${urlPublica}/sitemap.xml`, ''].join('\n'),
  );
});

/** sitemap.xml con el inicio, el catálogo, las colecciones y las piezas publicadas. */
app.get('/sitemap.xml', async (_req, res) => {
  try {
    const respuesta = await fetch(`${urlApi}/api/v1/seo/rutas`);
    if (!respuesta.ok) throw new Error(`API ${respuesta.status}`);
    const rutas = (await respuesta.json()) as { productos: string[]; categorias: string[] };
    const urls = [
      '/',
      '/catalogo',
      ...rutas.categorias.map((s) => `/catalogo/${encodeURIComponent(s)}`),
      ...rutas.productos.map((s) => `/producto/${encodeURIComponent(s)}`),
    ];
    const cuerpo = urls.map((u) => `  <url><loc>${urlPublica}${u}</loc></url>`).join('\n');
    res
      .type('application/xml')
      .set('Cache-Control', 'public, max-age=3600')
      .send(`<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${cuerpo}\n</urlset>\n`);
  } catch {
    res.status(503).type('text/plain').send('Sitemap no disponible temporalmente');
  }
});

/**
 * Serve static files from /browser
 */
app.use(
  express.static(browserDistFolder, {
    maxAge: '1y',
    index: false,
    redirect: false,
  }),
);

/**
 * Handle all other requests by rendering the Angular application.
 */
app.use((req, res, next) => {
  angularApp
    .handle(req)
    .then((response) =>
      response ? writeResponseToNodeResponse(response, res) : next(),
    )
    .catch(next);
});

/**
 * Start the server if this module is the main entry point, or it is ran via PM2.
 * The server listens on the port defined by the `PORT` environment variable, or defaults to 4000.
 */
if (isMainModule(import.meta.url) || process.env['pm_id']) {
  const port = process.env['PORT'] || 4000;
  app.listen(port, (error) => {
    if (error) {
      throw error;
    }

    console.log(`Node Express server listening on http://localhost:${port}`);
  });
}

/**
 * Request handler used by the Angular CLI (for dev-server and during build) or Firebase Cloud Functions.
 */
export const reqHandler = createNodeRequestHandler(app);
