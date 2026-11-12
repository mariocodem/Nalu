# Docker (entorno local)

El `docker-compose.yml` de la raíz levanta:

| Servicio | Imagen | Puertos | Para qué |
|---|---|---|---|
| `postgres` | `postgres:18` | 5433 en el host (`DB_PORT`) | Base de datos. Liquibase crea el esquema al arrancar el backend |
| `mailpit` | `axllent/mailpit` | 8025 (UI), 1025 (SMTP) | Recibe los correos de notificación en desarrollo |

```bash
cp .env.example .env        # y define DB_PASSWORD
docker compose up -d
docker compose ps           # ambos servicios en estado "healthy"
```

Los datos de PostgreSQL viven en el volumen `pgdata`. Para borrarlos: `docker compose down -v` (es destructivo).
