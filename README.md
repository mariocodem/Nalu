# Nalu

Plataforma de comercio electrónico especializada en joyería.

## Descripción

Nalu es una plataforma de comercio electrónico para
la comercialización de joyería, materiales y productos
personalizables.

## Requisitos

- JDK 25.
- Node.js 26 (versión fijada en `frontend/.nvmrc`).
- Docker con Docker Compose.

Si el JDK por defecto del sistema no es el 25, exporta `JAVA_HOME` antes de ejecutar Maven. Por ejemplo, en Arch Linux:

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk
```

## Arranque local

### 1. Variables de entorno

Copia el archivo de ejemplo y ajusta los valores:

```bash
cp .env.example .env
```

Valores que debes cambiar en `.env`:

- `DB_PASSWORD`: contraseña de PostgreSQL.
- `JWT_SECRET`: clave de firma. Genérala con `openssl rand -base64 32`.
- `NALU_ADMIN_PASSWORD`: contraseña del administrador inicial, de 12 caracteres o más.
- `GEMINI_API_KEY`: llave de la Gemini Developer API (Google AI Studio) para el módulo de IA. Es un secreto del servidor: nunca va al frontend ni a los logs.
- `GEMINI_MODEL` y `GEMINI_MODELOS_RESPALDO` (opcionales): modelo principal del asistente de compras y modelos que se usan si el principal está saturado. `NALU_ASISTENTE_MENSAJES` limita los mensajes por persona cada 10 minutos. Detalles en `docs/decisiones/005-asistente-ia.md`.

No subas `.env` al repositorio.

### 2. Servicios de infraestructura

```bash
docker compose up -d
```

Esto levanta PostgreSQL 18 en el puerto 5433 del host (configurable con `DB_PORT`) y Mailpit. La interfaz de Mailpit está en <http://localhost:8025> y su servidor SMTP escucha en el puerto 1025.

### 3. Backend

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

El perfil `dev` lee el `.env` de la raíz del repositorio y crea el administrador inicial con `NALU_ADMIN_EMAIL` y `NALU_ADMIN_PASSWORD`. La API queda disponible en <http://localhost:8080>.

### 4. Frontend

```bash
cd frontend
npm ci
npm start
```

La aplicación está en <http://localhost:4200>. El servidor de desarrollo redirige las peticiones `/api` a `http://localhost:8080` mediante `frontend/proxy.conf.json`.

### 5. Catálogo de demostración (solo para clase)

Con el backend en marcha:

```bash
node --env-file=.env scripts/demo/cargar-demo.mjs               # crea las piezas que falten
node --env-file=.env scripts/demo/cargar-demo.mjs --actualizar  # además sincroniza nombre, descripción y fotos de las existentes
```

Crea y publica 20 piezas con fotografías a través de la API de administración. Se puede ejecutar varias veces: salta los SKU que ya existen. Las fotos no se versionan:

- 14 son de Unsplash y Pexels (licencias de uso libre). El script las descarga solo si faltan; los créditos están en [scripts/demo/CREDITOS-FOTOS.md](scripts/demo/CREDITOS-FOTOS.md).
- Las de las otras 6 piezas y las editoriales del inicio (`frontend/public/editorial/`) son de terceros, solo para uso académico, y hay que copiarlas a mano. Sin la carpeta editorial, la portada y las categorías del inicio se ven sin fotografía.

Ninguna foto es de una pieza real de Nalu; precios y existencias son de ejemplo.

## Pruebas

Backend (necesita Docker porque usa Testcontainers):

```bash
cd backend
./mvnw verify
```

Frontend:

```bash
cd frontend
npm test -- --watch=false
npm run build
```

También se puede ejecutar `npx ng test --watch=false` desde `frontend`. La integración continua de `.github/workflows/ci.yml` usa estos mismos comandos.

## Operación

- `scripts/respaldo.sh`: respaldo de la base de datos con retención de 7 días. Prográmalo cada 24 horas (cron).
- `scripts/restaurar.sh <archivo.dump> [base]`: restaura un respaldo en una base nueva sin tocar la actual.
- `node scripts/prueba-carga.mjs [urlApi] [usuarios] [segundos]`: prueba de carga (RNF06).
- Pagos (ADR 006): la pasarela activa la elige `NALU_PASARELA` (`payu` por defecto, o `wompi`).
  - **PayU sandbox** funciona sin configurar nada: usa las credenciales públicas de prueba de PayU. Para probar, compra en la tienda, pulsa «Pagar» y paga en PayU con la tarjeta de prueba VISA `4097440000000004`:
    - Aprobado: titular `APPROVED`, CVV `777`, mes de vencimiento menor que 6 (por ejemplo, `05/2030`).
    - Rechazado: titular `REJECTED`, CVV `666`, mes de vencimiento mayor que 6 (por ejemplo, `07/2030`).

    Al volver a `/pago/resultado`, Nalu verifica la firma y actualiza el pedido.
  - **Wompi sandbox** usa las llaves `WOMPI_*` de `.env`. Configura en el panel de Wompi la URL de eventos `https://<tu-api>/api/v1/webhooks/wompi` (en local necesitas un túnel público, como `cloudflared` o `ngrok`).

## Cambios de esquema

El esquema lo gestiona Liquibase. Para generar un changeset a partir de los cambios en las entidades:

```bash
scripts/liquibase-diff.sh <descripcion-en-kebab-case>
```

El flujo es:

1. Modifica las entidades JPA en `backend/src/main/java/co/nalu/datos/entidad/`.
2. Ejecuta `scripts/liquibase-diff.sh <descripcion>`; el script levanta un PostgreSQL efímero, aplica los changesets existentes y compara el esquema.
3. Revisa a mano el changeset generado en `backend/src/main/resources/db/changelog/cambios/` antes de confirmarlo.
4. No edites nunca un changeset que ya se haya aplicado: crea uno nuevo para cada cambio.

## Documentación

- [CLAUDE.md](CLAUDE.md): instrucciones y reglas del proyecto.
- [Plan de desarrollo](docs/arquitectura/plan-desarrollo.md): arquitectura, modelo de datos y fases.
- Contratos de la API: [autenticación](docs/arquitectura/api-autenticacion.md), [fase 1](docs/arquitectura/api-fase1.md), [fase 2](docs/arquitectura/api-fase2.md), [fases 3 y 4](docs/arquitectura/api-fase3.md) y [fase 5](docs/arquitectura/api-fase5.md).
- [Informe de calidad](docs/calidad/informe-de-calidad.md): verificación de los requisitos no funcionales y pendientes para producción.
- [Análisis y diseño](docs/diseno/analisis-y-diseno-nalu.pdf): documento oficial del modelo de datos y los requisitos.
- [Decisiones](docs/decisiones/): registros de arquitectura (ADR).
- [Seguimiento de tareas](docs/seguimiento-tareas.md): estado de las tareas del proyecto.

## Tecnologías

### Backend

- Java
- Spring Boot
- Spring Security
- Spring Data JPA
- Liquibase

### Frontend

- Angular
- TypeScript
- Angular SSR
- SCSS

### Base de datos

- PostgreSQL

### Infraestructura

- Docker
- Docker Compose
- GitHub Actions

## Estructura

- backend/: API y lógica de negocio.
- frontend/: aplicación web.
- docs/: documentación.
- infraestructura/: Docker y base de datos.
- scripts/: automatizaciones.

## Estado del proyecto

MVP completo (Fases 0 a 6). Pendiente de validación del propietario y de las decisiones de despliegue: ver el informe de calidad.
