#!/usr/bin/env bash
# Corre las pruebas de Nalu, las publica en Kiwi TCMS y genera el informe de docs/calidad/kiwi/.
#
# Requisitos: Kiwi en marcha (docker compose -f infraestructura/kiwi/docker-compose.yml up -d),
# Nalu en marcha (API en :8080 y tienda en :4200) y el .env de la raíz.
#   scripts/kiwi/publicar.sh                  # corre backend y frontend, publica y genera el informe
#   SALTAR_PRUEBAS=1 scripts/kiwi/publicar.sh # reutiliza los JUnit XML de la última ejecución
#
# El usuario de Kiwi es KIWI_USUARIO / KIWI_CLAVE; sin ellos se crea «qa-nalu» con una clave
# aleatoria guardada fuera del repositorio (~/.cache/nalu-kiwi/clave).
set -euo pipefail

raiz="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$raiz"
# .env no se evalúa con la shell (es un archivo de propiedades, como lo lee Spring): lo lee el script de Python.

cache="${XDG_CACHE_HOME:-$HOME/.cache}/nalu-kiwi"
mkdir -p "$cache" && chmod 700 "$cache"
if [ ! -x "$cache/venv/bin/python" ]; then
  python3 -m venv "$cache/venv"
  "$cache/venv/bin/pip" install -q tcms-api==15.0 kiwitcms-junit.xml-plugin==15.0
fi

docker exec nalu-kiwi /Kiwi/manage.py migrate --noinput >/dev/null
# Un Kiwi recién instalado no trae clasificaciones de producto (las crea el asistente interactivo).
docker exec nalu-kiwi /Kiwi/manage.py shell -c \
  "from tcms.management.models import Classification; Classification.objects.get_or_create(name='Comercio electrónico')" >/dev/null
export KIWI_USUARIO="${KIWI_USUARIO:-qa-nalu}"
if [ -z "${KIWI_CLAVE:-}" ]; then
  if [ ! -s "$cache/clave" ]; then
    (umask 077; python3 -c "import secrets; print(secrets.token_urlsafe(18))" > "$cache/clave")
    docker exec -e DJANGO_SUPERUSER_PASSWORD="$(cat "$cache/clave")" nalu-kiwi /Kiwi/manage.py \
      createsuperuser --noinput --username "$KIWI_USUARIO" --email qa@nalu.local >/dev/null
  fi
  KIWI_CLAVE="$(cat "$cache/clave")"
fi
export KIWI_CLAVE

junit_frontend="$cache/frontend-junit.xml"
if [ "${SALTAR_PRUEBAS:-0}" != 1 ]; then
  (cd backend && ./mvnw -q verify)
  (cd frontend && npx ng test --watch=false --reporters=junit --reporters=default --output-file="$junit_frontend")
fi

JUNIT_BACKEND=backend/target/surefire-reports JUNIT_FRONTEND="$junit_frontend" SALIDA=docs/calidad/kiwi \
  "$cache/venv/bin/python" scripts/kiwi/publicar_kiwi.py
