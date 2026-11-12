# ADR 004: Dirección visual clara y editorial para la tienda pública

- **Estado:** Aceptada por el propietario el 2026-11-09
- **Fecha:** 2026-11-09
- **Reemplaza:** la dirección «La vitrina» (D-02), oscura y museográfica

## Contexto

El propietario pidió rediseñar el inicio de la tienda con la calidad de una joyería premium, tomando como referencias de principios (no de copia) a Mejuri (fotografía editorial), Blue Nile (claridad comercial), Quince (rejillas de producto limpias) y el diseño de Apple (precisión y contención). La dirección anterior era oscura; aplicarla solo al inicio habría dejado el catálogo, la ficha y la bolsa en otro mundo visual.

## Decisión

| Tema | Decisión |
|---|---|
| Alcance | Toda la tienda pública comparte el sistema: tokens globales en `frontend/src/styles.scss`, aviso de envío, cabecera, pie y tarjeta de pieza. El catálogo, la ficha, la bolsa y la cuenta lo heredan sin cambiar su comportamiento |
| Tono | Claro: blanco y piedra fría como suelo de la fotografía, tinta casi negra |
| Color de marca | Verde esmeralda profundo (`#0E3B2E`) en franjas completas (aviso, sala Esmeralda, pie); esmeralda `#0F6B4F` como color de interacción y foco |
| Tipografía | Bodoni Moda (titulares, óptica fija en 28 para que los trazos finos no se pierdan) y Hanken Grotesk (interfaz y texto). Se sirven desde `frontend/public/fuentes/` (subconjunto latino, licencia SIL OFL 1.1) en lugar de Google Fonts: la hoja externa bloqueaba el renderizado y en pruebas tardó más de 10 s en responder |
| Forma | Esquinas rectas, reglas de 1 px, sin sombras salvo en menús desplegables |
| Contenido | Solo datos reales: categorías, materiales y piezas de la API; condiciones de envío y reserva del ADR 003 (`core/tienda/condiciones-venta.ts`). Sin boletín (no hay integración), sin «más vendidos» (no hay dato público), sin devoluciones ni garantía (decisiones abiertas) |

El contrato de dirección (tesis, primer viewport, interacción firma) está en `.impeccable/surfaces/frontend-src-app-tienda-inicio-inicio-html.md`.

## Consecuencias

- Los nombres de tokens de «La vitrina» (`--muro`, `--cedula`, `--oro-mate`…) se reemplazaron por nombres semánticos (`--fondo`, `--piedra`, `--tinta`, `--acento`…) en todos los componentes.
- La tarjeta de pieza deja de mostrar el número de cédula («N.º 001»); su entrada `numero` se eliminó.
- Las fotografías editoriales (`frontend/public/editorial/`) y las de demostración (`scripts/demo/fotos/`) son de terceros y solo para uso académico: no se versionan. Antes de una publicación real hay que sustituirlas por fotografía propia (decisión abierta 9 de `CLAUDE.md` §12 sobre almacenamiento de imágenes).
- Las cifras de envío están duplicadas en el frontend porque el backend no las expone públicamente; si cambian `NALU_ENVIO_TARIFA` o `NALU_ENVIO_GRATIS_DESDE`, hay que cambiar `condiciones-venta.ts`. Un endpoint público de condiciones eliminaría la duplicación.
