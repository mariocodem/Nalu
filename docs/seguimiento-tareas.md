# Seguimiento de tareas

Formato y estados según `CLAUDE.md` §9: PENDIENTE, EN PROGRESO, EN REVISIÓN, BLOQUEADO, COMPLETADO.
Responsables: **C** = Claude, **A** = agente auxiliar (OpenCode vía Herdr, revisado por Claude), **P** = propietario.

**Estado:** MVP completo (F0 a F6, 2026-11-09). Pendiente de la validación del propietario y de las decisiones de despliegue (ver el informe de calidad). F0 aprobada el 2026-11-09; F1 completada el 2026-11-09 (el propietario pidió continuar sin pausa entre fases). Desde la F1, Claude trabaja solo: OpenCode agotó su cuota gratuita.

## Fase 0

| ID | Descripción | Resp. | Depende de | Estado | Archivos | Pruebas / resultado |
|---|---|---|---|---|---|---|
| F0-01 | Repo Git propio, normalizar carpetas, mover el plan a `docs/` | C | — | COMPLETADO | `.git`, `.github/`, `docs/`, `CLAUDE.md`, `README.md` | `git status` muestra solo archivos del proyecto |
| F0-02 | ADR 001 y 002 + este registro | C | F0-01 | COMPLETADO | `docs/decisiones/`, `docs/seguimiento-tareas.md` | ADR 001 aceptado con las versiones exactas fijadas (2026-11-09) |
| F0-03 | Docker Compose (PostgreSQL 18 + Mailpit) y `.env.example` | C | F0-01 | COMPLETADO | `docker-compose.yml`, `.env.example`, `infraestructura/docker/` | `docker compose up -d`: PostgreSQL 18.6 responde a `psql` en 127.0.0.1:5433 y Mailpit está *healthy* (UI con HTTP 200). El host usa 5433 porque ya hay un PostgreSQL del sistema en 5432 |
| F0-04 | Generar el backend (Boot 4.1.2, Java 25) | C | F0-01, P-01 | COMPLETADO | `backend/` | `./mvnw verify` con JDK 25.0.5; arranque con perfil `dev` contra PostgreSQL 18.6 en 8 s; `/actuator/health` 200 |
| F0-05 | Paquetes por capas + `ArquitecturaCapasTest` | C | F0-04 | COMPLETADO | `co/nalu/*/package-info.java`, `ArquitecturaCapasTest` | 9 reglas ArchUnit en verde. Prueba negativa: una dependencia api→datos y un `@Transactional` fuera de negocio hacen fallar 3 reglas |
| F0-06 | `application.yml` + `@ConfigurationProperties` | C | F0-04 | COMPLETADO | `application.yml`, `application-dev.yml`, `config/JwtProperties`, `config/AppProperties` | `JwtPropertiesTest` (3): rechaza claves de menos de 256 bits y no expone el secreto en `toString` |
| F0-07 | Spike de `liquibase-hibernate` con Hibernate 7 | C | F0-06 | COMPLETADO | `pom.xml` (plugin), `scripts/liquibase-diff.sh` | `liquibase-hibernate7` 5.0.4 genera el diff contra Hibernate 7.4.13. No hace falta el plan B. Un segundo diff tras aplicar los changesets da «Sin diferencias» |
| F0-08 | Entidades JPA (alcance F0: cuentas) | A + C | F0-05, F0-06 | COMPLETADO | `datos/entidad/` (Usuario, Cliente, Administrador, RefreshToken, EstadoUsuario) | `RefreshTokenTest` (5) y `MapeoCuentasTest` (3, PostgreSQL real). El propietario decidió dejar el resto de entidades para su fase (falta el PDF, P-02) |
| F0-09 | Primeros changesets Liquibase | C | F0-07, F0-08 | COMPLETADO | `db/changelog/cambios/` | Dos changesets revisados a mano (nombres de restricciones; `delete` previo a una columna NOT NULL). `ddl-auto=validate` arranca contra el esquema en Testcontainers y en `dev` |
| F0-10 | Validación YAVI + problem+json en español | C | F0-05 | COMPLETADO | `api/error/`, `negocio/comun/`, `validacion/mensajes.properties`, `messages.properties` | `FormateadorMensajesValidacionTest` (3: las 70 claves de YAVI traducidas) y `ManejadorErroresTest` (9: 400, 404, 405, 409, 403, 500) |
| F0-11 | Seguridad JWT (login, refresh, logout, me) | C | F0-08, F0-10 | COMPLETADO | `config/SeguridadConfig`, `negocio/cuentas/`, `api/publico/AutenticacionController`, `docs/arquitectura/api-autenticacion.md` | `AutenticacionTest` (15): token alterado o expirado → 401; rotación; reutilización → revoca la familia; inactividad de 30 min; `/admin/**` 401/403; CORS. Prueba manual en `dev` con curl directo y a través del proxy de Angular |
| F0-12 | Datos semilla | C | F0-08 | COMPLETADO | `negocio/cuentas/DatosSemillaRunner`, `config/AdministradorInicialProperties` | En `dev` crea el administrador desde `.env` (idempotente) y ese usuario inicia sesión. Materiales y categorías pasan a F1 |
| F0-13 | Generar el frontend Angular 22 con SSR | A | F0-01 | COMPLETADO | `frontend/` | Build, Vitest y SSR por petición (`RenderMode.Server`) con HTTP 200 y `lang="es"` |
| F0-14 | AuthService, interceptor y guard | A + C | F0-13, contrato de F0-11 | COMPLETADO | `frontend/src/app/core/auth/`, `app.config.ts` | 16 pruebas Vitest. Corrección del coordinador: un error del reintento ya no cierra la sesión. SSR no llama al backend |
| F0-15 | CI en GitHub Actions | A | F0-04, F0-13 | COMPLETADO | `.github/workflows/ci.yml` | Los dos jobs se reprodujeron en local con código 0. Falta ejecutarla en GitHub (P-06) |
| F0-16 | Revisión final y documentación | C + A | todas | COMPLETADO | `README.md`, `docs/` | Puerta final: backend 48 pruebas y frontend 17 en verde, build de producción, diff de esquema sin diferencias, integración frontend↔backend por el proxy. Pendiente de la aprobación del propietario |

## Fase 1

Contrato: `docs/arquitectura/api-fase1.md`. Decisiones del propietario (2026-11-09): PDF oficial `Nalu.docx.pdf`; pantallas funcionales con los tokens del prototipo aprobado y pulido visual al final; estados de producto `BORRADOR`, `PUBLICADO` y `ARCHIVADO`.

| ID | Descripción | Resp. | Depende de | Estado | Archivos | Pruebas / resultado |
|---|---|---|---|---|---|---|
| F1-01 | Entidades de catálogo, direcciones e inventario | A + C | F0 | COMPLETADO | `datos/entidad/` | OpenCode escribió las entidades y sus pruebas antes de agotar su cuota; Claude las revisó y corrigió `reemplazarMateriales` (Hibernate inserta antes de borrar: ahora fusiona) |
| F1-02 | Registro de clientes y bloqueo por fuerza bruta | C | F0 | COMPLETADO | `negocio/cuentas/`, `api/publico/` | `RegistroYFuerzaBrutaTest` (6): registro con sesión, 400 sin política, 409 correo repetido, bloqueo tras 5 fallos y reinicio tras login correcto |
| F1-03 | Changeset del modelo de la Fase 1 | C | F1-01, F1-02 | COMPLETADO | `db/changelog/cambios/` | Changeset revisado: nombres de PK y `ck_variante_producto_stock` (SQL documentado en el plan §5.1). Diff posterior sin diferencias |
| F1-04 | Admin de categorías y materiales + semilla | C | F1-03 | COMPLETADO | `negocio/catalogo/`, `api/admin/` | `CatalogoAdminTest`: árbol sin ciclos, slug repetido 409, materiales. Semilla de 5 materiales y 4 categorías en `dev` |
| F1-05 | Perfil y direcciones del cliente | C | F1-03 | COMPLETADO | `negocio/clientes/`, `api/cliente/` | `CuentaClienteTest` (5): perfil, direcciones, dirección ajena 404, admin sin perfil 403 |
| F1-06 | Admin de productos, materiales, variantes y estado | C | F1-04 | COMPLETADO | `negocio/catalogo/`, `api/admin/` | Criterio de aceptación en `CatalogoAdminTest`: 2 materiales + 3 tallas + imagen → publicado; movimientos `ENTRADA` de la existencia inicial; CHECK de existencias en BD |
| F1-07 | Imágenes de producto (almacenamiento local y endpoints) | C | F1-06 | COMPLETADO | `datos/archivo/`, `negocio/catalogo/` | Firma del archivo verificada (no se confía en el Content-Type); borrado del archivo tras el commit y limpieza si la transacción se revierte |
| F1-10 | Frontend: estructura base, login y registro | C | contrato | COMPLETADO | `frontend/src/app/` | Cabecera, inicio provisional, login y registro con errores por campo |
| F1-11 | Frontend: perfil y direcciones | C | F1-10, contrato | COMPLETADO | `frontend/src/app/cuenta/` | Página «Mi cuenta» con perfil y direcciones |
| F1-12 | Frontend: panel de administración (categorías y productos) | C | F1-10, contrato | COMPLETADO | `frontend/src/app/admin/` | Panel: productos (lista con filtros y editor con materiales, variantes, imágenes y publicación), categorías y materiales |
| F1-13 | Criterio de aceptación de punta a punta y documentación | C | todas | COMPLETADO | pruebas, docs | Backend 85 pruebas, frontend 24 pruebas, build y diff de esquema en verde |

## Fase 2

Contrato: `docs/arquitectura/api-fase2.md`. Decisiones de negocio: `docs/decisiones/003-decisiones-de-negocio.md`.

| ID | Descripción | Resp. | Depende de | Estado | Archivos | Pruebas / resultado |
|---|---|---|---|---|---|---|
| F2-01 | Precio de venta (ADR 003) y `precio_desde` derivado | C | F1 | COMPLETADO | `negocio/catalogo/CalculadoraPrecio`, changeset | Cambiar el factor de un metal recalcula todas sus piezas |
| F2-02 | Catálogo público con filtros, categorías y ficha | C | F2-01 | COMPLETADO | `CatalogoPublicoService`, `CatalogoController` | `CatalogoPublicoTest` (5): visibilidad, filtros (categoría con subcategorías, material, precio, disponibilidad, orden), ficha |
| F2-03 | SEO: metadatos editables, JSON-LD, sitemap y robots | C | F2-02 | COMPLETADO | `MetadatoSeo`, `ConstructorSeo`, `SeoAdminController`, `server.ts` | JSON-LD `Product` + `Offer`/`AggregateOffer`; prueba con SSR real: título, canonical y JSON-LD en el HTML inicial, `</script>` escapado, 404 real |
| F2-04 | Frontend de la tienda («La vitrina»): inicio, catálogo y ficha con SSR | C | F2-02 | COMPLETADO | `frontend/src/app/tienda/`, `shared/vitrina/` | Build y SSR verificados con un producto real (precio «Desde $ 250.000», talla agotada marcada) |
| F2-05 | Panel: edición de SEO de productos y categorías | C | F2-03 | COMPLETADO | `admin/seo/editor-seo.ts` | Build y prueba de API del guardado |

## Fases 3 y 4

Contrato: `docs/arquitectura/api-fase3.md`.

| ID | Descripción | Resp. | Depende de | Estado | Archivos | Pruebas / resultado |
|---|---|---|---|---|---|---|
| F3-01 | Modelo de carrito, cupones, pedidos, historial, pagos y eventos | C | F2 | COMPLETADO | `datos/entidad/`, changeset | Diff sin diferencias |
| F3-02 | Carrito de visitante y cliente con fusión al iniciar sesión | C | F3-01 | COMPLETADO | `negocio/carrito/`, `api/publico/CarritoController` | `CompraTest`: token de visitante, tope de existencias, fusión |
| F3-03 | Cotización, cupones y envío (ADR 003) | C | F3-01 | COMPLETADO | `negocio/pedidos/`, `negocio/promociones/` | Cupón del 10 %, envío $15.000 y gratis desde $200.000 |
| F3-04 | Checkout atómico con reserva y bloqueo de filas | C | F3-02 | COMPLETADO | `CheckoutService`, `InventarioService` | **Criterio de aceptación**: dos compras simultáneas de la última unidad → 201 y 409. RNF10: una línea agotada no deja pedido ni reserva |
| F4-01 | Wompi: checkout web con firma de integridad y webhook firmado e idempotente | C | F3-04 | COMPLETADO | `integracion/wompi/`, `negocio/pagos/`, `api/webhook/` | Aprobado, rechazado + reintento, repetido (un solo descuento), firma alterada 401, monto distinto |
| F4-02 | Reservas vencidas y cancelación a las 24 h | C | F4-01 | COMPLETADO | `TareasPedidos` | Prueba con fechas adelantadas |
| F4-03 | Notificaciones por correo tras el commit | C | F4-01 | COMPLETADO | `negocio/notificaciones/`, `integracion/correo/` | Correo «Recibimos tu pago» en Mailpit en la prueba de punta a punta |
| F3-05 | Frontend: bolsa, checkout, resultado del pago, mis pedidos, cupones en el panel | C | F3-04 | COMPLETADO | `tienda/bolsa`, `tienda/checkout`, `tienda/pago`, `cuenta/pedidos`, `admin/promociones` | 25 pruebas Vitest; compra de punta a punta con servidores reales |
| P-04 | Verificar un pago real en el sandbox de Wompi | P | F4-01 | PENDIENTE | `.env` | Requiere las llaves sandbox del propietario |

## Fase 5

Contrato: `docs/arquitectura/api-fase5.md`.

| ID | Descripción | Resp. | Depende de | Estado | Archivos | Pruebas / resultado |
|---|---|---|---|---|---|---|
| F5-01 | Gestión de pedidos y envíos según la máquina de estados | C | F4 | COMPLETADO | `GestionPedidoService`, `PedidoAdminController`, `Envio` | **Criterio de aceptación** en `OperacionTest`: PENDIENTE_PAGO → PAGADO → EN_PREPARACION → ENVIADO → ENTREGADO; transiciones prohibidas → 409; cancelación pagada con motivo y reintegro |
| F5-02 | Movimientos manuales de inventario y alertas | C | F4 | COMPLETADO | `InventarioAdminService` | No se pueden retirar unidades reservadas; historial con responsable |
| F5-03 | Reseñas con compra verificada y moderación | C | F5-01 | COMPLETADO | `Resena`, `ResenaService` | Solo tras la entrega, una por producto; la ficha y el JSON-LD (`aggregateRating`) muestran solo las aprobadas |
| F5-04 | Reportes JSON y CSV | C | F5-01 | COMPLETADO | `ReporteRepository` (JPQL + proyecciones), `ReporteService` | El reporte de ventas por material refleja la venta; CSV con BOM y protección contra fórmulas |
| F5-05 | Frontend: panel de pedidos, inventario, reseñas y reportes; reseñas en la ficha y en mis pedidos | C | F5-01…04 | COMPLETADO | `admin/pedidos`, `admin/inventario`, `admin/resenas`, `admin/reportes` | Build y 25 pruebas Vitest en verde |

## Fase 6

Informe: `docs/calidad/informe-de-calidad.md`.

| ID | Descripción | Resp. | Depende de | Estado | Archivos | Pruebas / resultado |
|---|---|---|---|---|---|---|
| F6-01 | Cobertura con umbral obligatorio (RNF13) | C | F5 | COMPLETADO | `pom.xml` (JaCoCo) | 86 % total; pedidos 85,9 %, inventario 91,5 %, promociones 75 %; el build falla por debajo del 60 % |
| F6-02 | Prueba de carga con 50 usuarios (RNF06) | C | F5 | COMPLETADO | `scripts/prueba-carga.mjs` | 35.290 peticiones, 0 errores, p95 de 26 ms |
| F6-03 | Respaldo y restauración (RNF12) | C | F5 | COMPLETADO | `scripts/respaldo.sh`, `scripts/restaurar.sh` | Restauración en 1 s con conteos idénticos |
| F6-04 | Auditoría de acciones del panel y cambios de precio (RNF15) | C | F5 | COMPLETADO | `Auditoria`, `AuditoriaInterceptor`, `admin/auditoria` | `OperacionTest`: acción con usuario y cambio de precio con su valor anterior |
| F6-05 | Accesibilidad automática y cabeceras de seguridad | C | F5 | COMPLETADO | `server.ts`, plantillas | axe-core sin violaciones en las páginas SSR (la auditoría encontró y corrigió una plantilla frágil); contraste ≥ 5,5:1 |
| F6-06 | Verificación manual en navegadores, Lighthouse y usabilidad (RNF08, RNF09, RNF14) | P | F6-05 | PENDIENTE | — | No hay navegadores en el equipo de desarrollo |
| F6-07 | Informe de calidad y documentación final | C | todas | COMPLETADO | `docs/calidad/`, `README.md`, `.env.example` | — |

## Diseño visual (adelanto aprobado, sin tocar `frontend/`)

| ID | Descripción | Resp. | Depende de | Estado | Archivos | Pruebas / resultado |
|---|---|---|---|---|---|---|
| D-01 | `PRODUCT.md`: verdad del producto (cliente, posicionamiento, restricciones) | C | — | COMPLETADO | `PRODUCT.md` | Respuestas del propietario: compra para sí misma, origen colombiano, sin activos de marca |
| D-02 | Dirección visual del inicio de la tienda | C | D-01 | COMPLETADO (reemplazada por D-06) | `.impeccable/surfaces/` | El propietario eligió «La vitrina» (museografía de sala, cédulas como panel de compra) |
| D-03 | Prototipo HTML del inicio | C | D-02 | EN REVISIÓN | `docs/diseno/prototipo-inicio/index.html` | Capturas 1440/1280/390 y estados (metal, talla agotada, bolsa). Revisión independiente con 8 correcciones aplicadas Reemplazado por D-06: queda como referencia del mundo anterior |
| D-04 | `DESIGN.md` del mundo visual | C | D-06 | EN REVISIÓN | `DESIGN.md`, `.impeccable/design.json` | Derivado del código construido del mundo claro (ADR 004); pendiente de aprobación visual del propietario |
| D-05 | Catálogo de demostración con fotografías para la presentación en clase (fotos de terceros, no versionadas) | C | F1, F2 | COMPLETADO | `scripts/demo/cargar-demo.mjs`, `.gitignore`, `README.md` | 9 piezas publicadas por la API; segunda ejecución sin duplicados; capturas de inicio, catálogo y ficha revisadas |
| D-06 | Rediseño editorial claro del inicio y del marco de la tienda (ADR 004) | C | D-05 | EN REVISIÓN | `styles.scss`, `shared/cabecera/`, `shared/pie/`, `shared/vitrina/`, `tienda/inicio/`, `public/fuentes/`, `public/editorial/` | 23/23 pruebas funcionales en navegador, 25/25 unitarias, build de producción, axe 0 violaciones, sin desbordamiento en 360–1440. Informe: `.taste-skill/informe-comparativo.md` |

## Asistente de compras con IA (ADR 005)

| ID | Descripción | Resp. | Depende de | Estado | Archivos | Pruebas / resultado |
|---|---|---|---|---|---|---|
| IA-01 | Puerto `ModeloConversacional`, servicio con herramientas (catálogo, condiciones, pedidos del cliente) y límite por persona | C | F5 | COMPLETADO | `negocio/asistente/`, `config/AsistenteProperties` | `AsistenteApiTest` (7): tarjetas con datos del catálogo, herramientas de pedidos solo con rol CLIENTE, validación, 429, 503 |
| IA-02 | Adaptador de Gemini (`generateContent` con funciones, firmas de razonamiento, respaldo y pausa de modelos saturados) | C | IA-01 | COMPLETADO | `integracion/gemini/` | `GeminiModeloConversacionalTest` (8); formato verificado contra la API real |
| IA-03 | Endpoints públicos y seguridad | C | IA-01 | COMPLETADO | `api/publico/AsistenteController`, `SeguridadConfig`, `ManejadorErrores` | `ArquitecturaCapasTest` en verde |
| IA-04 | Chat en la tienda: botón «Asesora», panel, tarjetas con «Añadir a la bolsa» | C | IA-03 | EN REVISIÓN | `frontend/src/app/shared/asistente/` | Vitest 28/28; 12 comprobaciones en navegador con Gemini real (escritorio y móvil). Falta la validación visual del propietario |
| IA-05 | Política de datos: mencionar el tratamiento por Google y revisar los términos de la Gemini API antes de producción | P | IA-04 | PENDIENTE | — | — |

## Tareas del propietario

| ID | Descripción | Necesaria para | Estado |
|---|---|---|---|
| P-01 | Instalar el JDK 25 | F0-04 | COMPLETADO |
| P-02 | Copiar el PDF de análisis y diseño en `docs/diseno/` | Entidades de F1 en adelante | COMPLETADO (`docs/diseno/analisis-y-diseno-nalu.pdf`, versión del 2026-11-09) |
| P-03 | Aprobar ADR 001 | F0-04 | COMPLETADO |
| P-04 | Cuenta sandbox de Wompi + llaves (solo en `.env`) | F4 | PENDIENTE |
| P-05 | Decisiones de negocio de `CLAUDE.md` §12 (precio, envío, TTL de la reserva, número de pedido…) | F3 | COMPLETADO (ADR 003) |
| P-06 | Repositorio remoto en GitHub (opcional) | CI real | PENDIENTE |
