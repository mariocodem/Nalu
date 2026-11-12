# Rediseño del inicio de Nalu: comparison report

Date: 2026-11-09 · Owner decisions: light editorial world for the whole public store (ADR 004), replacing the dark "La vitrina" direction.

## Summary

The landing page (`/`) moves from a museum showcase on a black wall to an editorial jewelry magazine you can buy from:

- **Shipping announcement bar** in deep emerald.
- **Header** with a "Joyería" menu (real categories and materials plus an emerald panel with a photo), inline categories, search connected to the catalog, account menu, bag with counter, and a mobile drawer.
- **Full-bleed hero** with the headline over the photo.
- **Shop by category** with photography.
- **Esmeralda room**: a full emerald band with the API's emerald pieces.
- **Recién llegadas** ("Just arrived"): the newest pieces, without repeating the room's pieces.
- **"Conoce cada metal"** ("Get to know each metal"): a ruled list with the definition of each metal.
- **"Comprar en Nalu"** ("Buying at Nalu"): conditions from ADR 003 on a stone band.
- **Footer** in emerald.

The catalog, product page, bag and account inherit the new system without changing behavior.

## Modified files

| File | Change |
|---|---|
| `frontend/src/styles.scss` | New tokens (white, stone, ink, emerald), self-hosted fonts, inputs, buttons, focus, selection, scrollbar |
| `frontend/src/index.html` | Font preload; the Google Fonts stylesheet is removed |
| `frontend/src/app/app.ts` | Adds the footer; flex shell so the footer reaches the bottom |
| `frontend/src/app/shared/cabecera/cabecera.{ts,html,scss}` | Rewritten: announcement, navigation, "Joyería" menu, search, account, mobile drawer |
| `frontend/src/app/shared/pie/pie.ts` | **New**: footer |
| `frontend/src/app/shared/vitrina/vitrina-pieza.ts` | 4:5 card with name, material and price; the museum-label number is removed |
| `frontend/src/app/tienda/inicio/inicio.{ts,html,scss}` | Rewritten: sections, loading/empty/error states with retry |
| `frontend/src/app/core/tienda/condiciones-venta.ts` | **New**: shipping and reservation figures (ADR 003) |
| `frontend/src/app/tienda/catalogo/catalogo.{html,scss}` | Removes the `numero` binding; filter actions on a single row |
| `frontend/src/app/tienda/producto/ficha-producto.{html,scss}` | Removes the eyebrow above the name |
| `frontend/src/app/tienda/bolsa/bolsa.ts` | Empty-bag copy |
| 12 components (admin, account, checkout, product page…) | Mechanical rename of tokens to semantic names |
| `frontend/public/fuentes/` | **New**: Bodoni Moda and Hanken Grotesk (Latin subset, OFL) |
| `frontend/public/editorial/` | **New, not versioned**: hero, categories and menu photos (third-party, academic use) |
| `docs/decisiones/004-direccion-visual-tienda.md`, `docs/seguimiento-tareas.md`, `README.md`, `.gitignore` | Documentation |
| `DESIGN.md`, `.impeccable/design.json` | **New**: visual system documented from the built code (tokens, typography, rules) |
| `.impeccable/surfaces/frontend-src-app-tienda-inicio-inicio-html.md` | Landing direction contract |

## Design decisions

- **Typography:** Bodoni Moda for headlines, with optical size fixed at 28 because at 96 the hairlines disappear and "H" read as "I I". Hanken Grotesk for the interface.
- **Color:** emerald only in full bands, plus the focus ring. Interactions use ink.
- **Form:** square corners, 1 px rules, no shadows except in dropdowns.
- **Product grids:** "Recién llegadas" only shows full rows (multiples of 4); "Ver todas" ("See all") leads to the rest.
- **Self-hosted fonts:** Google Fonts sometimes took more than 10 s to respond and blocked rendering.
- **Omitted sections:**
  - Newsletter: no integration exists.
  - "Best sellers": there's no public data for it.
  - Returns and warranty: open business decisions.
  - A separate lifestyle section: it would have repeated photos.

## Problems found and resolved

1. **The backend wouldn't start.** `.env` had been changed:
   - `DB_PASSWORD` no longer matched the database. It was aligned with an `ALTER USER`, with the owner's approval.
   - `NALU_ADMIN_PASSWORD` has 9 characters and the backend requires 12. Pending for the owner; the backend ran with a temporary override that doesn't create users.
2. **Unreadable headline:** the hero's Bodoni at maximum optical size.
3. **Crowded mobile header:** the wordmark overlapped the icons, and the announcement wrapped onto two lines.
4. **Slow page:** the Google Fonts stylesheet blocked loading for more than 10 s.
5. **Search focus:** opening search didn't put focus in the field.
6. **The forms module in the initial bundle:** `FormsModule` loaded in the header (+0.5 MB of development JavaScript). Replaced with a native form.
7. **Independent review (disposition "fix"):** 8 material findings were applied, among them:
   - photo in the "Joyería" menu;
   - emerald only in bands;
   - metals headline with no promise;
   - footer without an origin claim and with a label for the demo photos;
   - eyebrow and arrow glyph removed;
   - trust strip rebuilt;
   - full rows;
   - hero at 78% of the screen;
   - catalog filters on one row;
   - footer at the bottom of short pages.

## Tests run

- **Functional (Playwright, Chromium), 23/23 in three consecutive runs:**
  - header search;
  - "Joyería" menu: opens, closes with Escape and with an outside click, and navigates;
  - category, Esmeralda room, "Ver novedades" anchor, piece card;
  - add to bag and header counter;
  - signup with validation;
  - account menu, protected "Pedidos" page, logout;
  - login with wrong and correct credentials;
  - mobile drawer.
- **States:** API error with "Reintentar" ("Retry"), skeleton while loading (`aria-busy`), recovery after retrying.
- **Unit tests:** 25/25 (Vitest).
- **Production build:** OK, with 2 budget warnings (see pending items).
- **Accessibility:** axe-core WCAG 2.2 A/AA on the landing page at 1440 and 390.
- **Design detector** (impeccable): no findings.

## Comparison table

Same conditions before and after: Angular development server (`ng serve` with proxy to the backend), Chromium 1248 from Playwright, the same database and the same scripts (`auditar.mjs`, `faro.sh`). Lighthouse ran twice per mode; both runs are shown.

| Metric | Before | After |
|---|---|---|
| Lint errors | No ESLint configured. `tsc --noEmit`: 0 errors. Prettier: 55 files with format differences | `tsc --noEmit`: 0 errors. Prettier not run on the final code |
| Performance, mobile (Lighthouse) | 36 / 39 | 39 / 40 |
| Performance, desktop (Lighthouse) | 62 / 73 | 58 / 60 |
| LCP, mobile | 18.0 s / 18.0 s | 13.1 s / 13.0 s |
| LCP, desktop | 5.7 s / 6.2 s | 4.2 s / 3.6 s |
| TBT, desktop | 330 / 130 ms | 450 / 470 ms |
| Page weight (desktop) | 7,216 KiB | 7,490 KiB |
| Accessibility (Lighthouse) | 100 | 100 |
| axe-core violations (1440 / 390) | 0 / 0 | 0 / 0 |
| Best practices / SEO (Lighthouse) | 96 / 100 | 96 / 100 |
| Responsive: horizontal overflow at 360, 390, 768, 1280, 1440 | 0 px at every width | 0 px at every width |
| Working landing-page links (1440 and 390) | 23/23 | 22/22 |
| Working forms | Landing: 0 forms. Signup and login weren't functionally tested before | Landing: header search (new). Signup, login and search verified (23/23 suite) |
| Console errors (13 page loads) | 13, all the session `401` on load (no cookie) | 13, the same `401` |
| Broken images | 0 | 0 |

**How to read the numbers:**
- **Desktop performance:** it drops because blocking time goes up with the larger header and footer. In development mode those numbers are inflated because the JavaScript isn't minified (about 5.5 MB of the weight is development JavaScript). LCP, which depends on the hero, improves on both mobile and desktop.
- **Links:** the before audit counted 23 links and the after audit 22, all working in both. The difference is product cards: "Recién llegadas" now shows only full rows (4 of the 7 available, with "Ver todas" for the rest). The header menu and the categories add links, and the "Joyería" menu and search panels add more that only exist when they're open.
- **Measurement method:** the before audit waited for `networkidle`. The after audit waits for `load` and then `networkidle` with a limit, because the slow Google Fonts responses made the strict wait time out. That doesn't affect overflow, links or axe.

## Independent review

Two rounds, both with the disposition "fix".

**Round 1:** 8 material findings.

**Round 2 (scoring the fixes):**
- Resolved: the menu photo, emerald only in bands, the metals headline, the floor bans and the inherited pages.
- Partial: the footer and SEO (the footer is fixed; the SEO title stays open) and the composition (the fold).
- Two regressions from the batch:
  - the Rings tile showed necklaces;
  - the mobile drawer didn't cover the whole page.

The fold and both regressions were then fixed and recaptured. Functional tests 23/23, overflow 0, axe 0. The reviewer didn't score those last fixes; under the skill, a third round needs the owner's approval.

Open from the review:
- **Seed reference:** the reviewer's contract asks for a reference from the `concept-seed` direction roll. It wasn't run because the owner's brief pinned the direction (references and tone), and the skill's new-work reference says a pinned direction "beats the roll". The two documents conflict; the roll can be run just to record provenance if the owner wants.
- **SEO title** "Joyería colombiana" (see below).

## Pending items

- **Admin password:** set `NALU_ADMIN_PASSWORD` in `.env` to 12 characters or more. Until then the backend only starts with an override.
- **SEO title:** `inicio.ts` keeps "Joyería colombiana", copy that predates the redesign. PRODUCT.md says the origin isn't documented yet; the owner decides whether to keep it.
- **Duplicated shipping figures:** they're duplicated in `condiciones-venta.ts` because the backend doesn't expose them. A public endpoint would remove the duplication.
- **Repeated photos:** only 7 source photos exist, so the category tiles and the product cards share photos. Variety needs more photography.
- **CSS budget:** warning in `cabecera.scss` (6.4 kB) and `inicio.scss` (6.1 kB) against a 4 kB warning threshold. Below the 8 kB error.
- **Test data:**
  - the test product "Anillo Selva 4966" has a placeholder image;
  - the test customers `prueba.rediseno+…@nalu.local` were created by the functional tests.
- **Production measurement:** performance hasn't been measured on the production SSR build because it doesn't proxy `/api` locally.

## Screenshots

- Before: `.taste-skill/before/`
  - `inicio-{1440,1280,768,390,360}.png`
  - `catalogo-*`, `ficha-*`, `bolsa-*`, `login-*`
  - `lighthouse-*.json`, `informe.json`
- After: `.taste-skill/after/`
  - the same files;
  - `estado-menu-joyeria-1440.png`, `estado-busqueda-1440.png`, `estado-menu-movil-390.png`, `estado-error.png`, `estado-cargando.png`
