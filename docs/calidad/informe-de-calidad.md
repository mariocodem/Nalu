# Informe de calidad (Fase 6)

- **Fecha:** 2026-11-09
- **Alcance:** MVP completo (Fases 0–5) en el entorno local: backend Spring Boot 4.1.2 / Java 25, frontend Angular 22 con SSR y PostgreSQL 18.
- **Fuente de los requisitos:** `docs/diseno/analisis-y-diseno-nalu.pdf`, Tabla de requerimientos no funcionales (RNF01–RNF15).

## 1. Resumen

| Verificación | Resultado |
|---|---|
| Pruebas del backend (`./mvnw verify`) | **105 pruebas, 0 fallos**: unitarias, de arquitectura (ArchUnit) e integración con PostgreSQL real (Testcontainers) |
| Pruebas del frontend (`ng test`) | **25 pruebas, 0 fallos** (Vitest) |
| Cobertura (JaCoCo, el build falla por debajo del umbral) | **86 % de líneas en total**. Pedidos 85,9 %, inventario 91,5 %, promociones 75,0 % (mínimo exigido: 60 %) |
| Carga: 50 usuarios concurrentes, 60 s | **35.290 peticiones, 0 errores, p95 global de 26 ms** (máximo permitido: 2.000 ms) |
| Respaldo y restauración | Respaldo de 68 KB; restauración completa en **1 s** en una base nueva, con conteos idénticos |
| Accesibilidad automática (axe-core, WCAG 2.2 A/AA) en las páginas SSR | **0 violaciones** en inicio, catálogo, colección, ficha y 404 |
| Contraste de la paleta (cálculo WCAG) | Todos los pares de texto **≥ 5,5:1** |
| Esquema de base de datos | Siete changesets; el diff entre las entidades y la base da «Sin diferencias» |

## 2. Requisitos no funcionales

| RNF | Requisito | Verificación | Estado |
|---|---|---|---|
| RNF01 | Contraseñas seguras y sesión que expira | BCrypt (factor 12), JWT de 15 min, refresh rotativo que caduca a los 30 min sin uso (`AutenticacionTest`); bloqueo tras 5 intentos fallidos (`RegistroYFuerzaBrutaTest`) | Cumple |
| RNF02 | Panel solo para administradores | `/admin/**` sin token → 401 y con rol cliente → 403 (`AutenticacionTest`, `CatalogoAdminTest`, `CuentaClienteTest`); recursos ajenos → 404 | Cumple |
| RNF03 | Comunicación cifrada (HTTPS) | La cookie `nalu_rt` es `Secure` fuera de `dev`; HSTS lo añade Spring Security sobre HTTPS | **Depende del despliegue**: el proxy de producción debe forzar HTTPS (hosting pendiente) |
| RNF04 | Nalu no recibe datos de tarjeta | El pago ocurre en el checkout web de Wompi; ninguna tabla guarda datos de tarjeta | Cumple |
| RNF05 | Ley 1581 | Aceptar la política es obligatorio y queda la fecha (`politica_aceptada_en`) | Cumple en lo técnico; **falta el texto de la política** (propietario) |
| RNF06 | Respuesta ≤ 2 s con 50 usuarios | `scripts/prueba-carga.mjs`: p95 de 26 ms, sin errores | Cumple en local. Limitaciones: catálogo con pocos productos; la creación de pedidos se verificó en las pruebas de integración, no bajo carga |
| RNF07 | SEO e interoperabilidad | SSR por petición, título, meta, canonical, JSON-LD `Product`/`Offer`/`AggregateOffer`/`AggregateRating`, sitemap y robots | Cumple. Validador oficial de Schema.org: pendiente con la URL pública |
| RNF08 | Compra en ≤ 5 pasos | Ficha → bolsa → finalizar compra → pago en Wompi → resultado (4 pasos con sesión iniciada) | Cumple en diseño. La prueba con 5 usuarios es del propietario |
| RNF09 | Accesibilidad WCAG 2.2 AA | axe-core sin violaciones en las páginas SSR; etiquetas en todos los campos; `alt` obligatorio; foco visible; contraste calculado | Cumple en lo automático. Falta Lighthouse en un navegador real y una revisión con teclado de las páginas que solo se renderizan en el cliente (cuenta, checkout, panel) |
| RNF10 | Atomicidad del pedido | Un fallo a mitad del checkout no deja pedido ni reserva (`CompraTest`); CHECK de existencias en la base | Cumple |
| RNF11 | Concurrencia sobre la última unidad | Dos compras simultáneas → 201 y 409 (`CompraTest`) | Cumple |
| RNF12 | Disponibilidad y respaldo | `scripts/respaldo.sh` (diario, retención de 7 días) y `scripts/restaurar.sh` (restauración en 1 s) | Cumple lo verificable en local. La disponibilidad del 99 % y la programación diaria (cron) dependen del hosting |
| RNF13 | Mantenibilidad | Capas verificadas por ArchUnit en cada build; cobertura ≥ 60 % obligatoria; CI en GitHub Actions | Cumple. La CI real requiere el remoto (P-06) |
| RNF14 | Compatibilidad (Chrome, Firefox, Safari, Edge; 360–1920 px) | Diseño móvil primero con rejillas `auto-fill` y tablas con desplazamiento propio | **Pendiente de verificación manual** en los cuatro navegadores y en 360/768/1366 px (no hay navegadores en el equipo de desarrollo) |
| RNF15 | Trazabilidad | Historial de estados de pedidos y movimientos de inventario con responsable y motivo; tabla `auditoria` con cada acción de escritura del panel y cada cambio de precio con su valor anterior (`OperacionTest`) | Cumple. Sin borrado automático (retención ≥ 1 año) |

## 3. Seguridad

- **Autenticación:** JWT HS256 firmado y validado por Spring Security, con emisor comprobado y una clave de 256 bits o más (el arranque falla con una más corta). El refresh token se guarda como hash SHA-256; si se reutiliza uno ya rotado, se revoca toda su familia.
- **Pagos:** la firma de los eventos de Wompi se compara en tiempo constante. Los eventos son idempotentes (`evento_pasarela.clave_idempotencia` es única) y el monto se verifica antes de aprobar.
- **Entradas:** YAVI valida en la API y Bean Validation en las entidades, sin SQL nativo (ArchUnit lo impide; las únicas excepciones son los dos CHECK documentados). Las imágenes se validan por su firma binaria. Los CSV llevan protección contra la inyección de fórmulas.
- **Cabeceras:** el SSR envía `nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy`, `Permissions-Policy` y una CSP (`frame-ancestors`, `form-action`, `object-src`, `base-uri`). La API envía las de Spring Security.
- **JSON-LD:** se escapa `<` para que una descripción con `</script>` no pueda inyectar código; está verificado con una prueba real en el SSR.
- **Pendiente:** CSP completa con nonces (requiere configurar `ngCspNonce` en el despliegue) y limitación de peticiones en el proxy de producción.

## 4. Cómo repetir las verificaciones

```bash
cd backend && ./mvnw verify                      # pruebas + umbral de cobertura (informe en target/site/jacoco)
cd frontend && npx ng test --watch=false && npm run build
scripts/liquibase-diff.sh comprobacion           # debe responder «Sin diferencias»
node scripts/prueba-carga.mjs http://localhost:8080 50 60   # con el backend en marcha
scripts/respaldo.sh && scripts/restaurar.sh respaldos/<archivo>.dump
```

## 5. Pendientes para la puesta en producción

1. **Decisiones del propietario** (CLAUDE.md §12): almacenamiento definitivo de imágenes (9), hosting (10), dominio (11), política de devoluciones (8) y texto de la política de datos.
2. **Pago real en el sandbox de Wompi** con las llaves del propietario (P-04).
3. **Repositorio remoto** para que corra la CI (P-06).
4. **Validación manual:** navegadores y anchos de RNF14, Lighthouse, prueba de usabilidad de RNF08 y validación visual del diseño (pulido final del panel con DESIGN.md, D-04).
5. **Configuración de producción:** HTTPS, `allowedHosts` del SSR con el dominio real, `NALU_URL_PUBLICA`, `APP_FRONTEND_URL`, cron de respaldos y su copia fuera del servidor.
