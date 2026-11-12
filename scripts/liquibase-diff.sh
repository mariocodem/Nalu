#!/usr/bin/env bash
# Genera un changeset de Liquibase a partir de los cambios en las entidades JPA (plan §5.1).
#
#   scripts/liquibase-diff.sh <descripcion-en-kebab-case>
#
# 1. Levanta un PostgreSQL 18 efímero (no toca la base de datos de desarrollo).
# 2. Le aplica los changesets existentes.
# 3. Compara ese esquema con las entidades de co.nalu.datos.entidad y escribe la diferencia en
#    backend/src/main/resources/db/changelog/cambios/<fecha>-<descripcion>.yaml
#
# El changeset generado SIEMPRE se revisa a mano antes de confirmarlo. Nunca se edita un changeset
# que ya se haya aplicado en algún entorno.
set -euo pipefail

descripcion=${1:?"Uso: scripts/liquibase-diff.sh <descripcion-en-kebab-case>"}
[[ $descripcion =~ ^[a-z0-9]+(-[a-z0-9]+)*$ ]] || { echo "La descripción debe estar en kebab-case" >&2; exit 1; }

raiz=$(cd "$(dirname "$0")/.." && pwd)
cd "$raiz/backend"
export JAVA_HOME=${JAVA_HOME_25:-${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk}}

contenedor="nalu-liquibase-diff-$$"
docker run -d --rm --name "$contenedor" \
  -e POSTGRES_DB=nalu -e POSTGRES_USER=nalu -e POSTGRES_PASSWORD=nalu \
  -p 127.0.0.1::5432 postgres:18 >/dev/null
trap 'docker stop "$contenedor" >/dev/null 2>&1 || true' EXIT

# Durante la inicialización PostgreSQL solo escucha por socket; esperar a TCP evita falsos positivos.
for _ in $(seq 1 60); do
  docker exec "$contenedor" pg_isready -q -h 127.0.0.1 -U nalu -d nalu && break
  sleep 1
done
puerto=$(docker port "$contenedor" 5432/tcp | head -1 | awk -F: '{print $NF}')
conexion=(-Dliquibase.url="jdbc:postgresql://127.0.0.1:$puerto/nalu" -Dliquibase.username=nalu -Dliquibase.password=nalu)

archivo="src/main/resources/db/changelog/cambios/$(date +%Y%m%d%H%M%S)-$descripcion.yaml"
mkdir -p "$(dirname "$archivo")"

./mvnw -B -q compile
./mvnw -B -q liquibase:update "${conexion[@]}"
./mvnw -B -q liquibase:diff "${conexion[@]}" -Dliquibase.diffChangeLogFile="$archivo"

if [[ -f $archivo ]]; then
  echo "Changeset generado: backend/$archivo"
  echo "Revísalo antes de confirmarlo (restricciones CHECK, índices, nombres)."
else
  echo "Sin diferencias: las entidades ya coinciden con los changesets."
fi
