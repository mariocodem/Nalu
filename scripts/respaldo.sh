#!/usr/bin/env bash
# Respaldo de la base de datos de Nalu (RNF12): pg_dump en formato personalizado, con retención.
#
#   scripts/respaldo.sh                 # respaldo en ./respaldos/ y borra los de más de 7 días
#   NALU_RESPALDOS=/ruta RETENCION_DIAS=14 scripts/respaldo.sh
#
# Programarlo cada 24 horas, p. ej. con cron:  15 3 * * *  /ruta/a/Nalu/scripts/respaldo.sh
# Usa el contenedor de docker-compose; en producción, apúntalo al servidor de base de datos real.
set -euo pipefail

raiz=$(cd "$(dirname "$0")/.." && pwd)
destino=${NALU_RESPALDOS:-$raiz/respaldos}
retencion=${RETENCION_DIAS:-7}
contenedor=${NALU_CONTENEDOR_BD:-nalu-postgres}
mkdir -p "$destino"
chmod 700 "$destino"

archivo="$destino/nalu-$(date +%Y%m%d-%H%M%S).dump"
docker exec "$contenedor" sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom --no-owner' > "$archivo.parcial"
mv "$archivo.parcial" "$archivo"   # un respaldo a medias nunca queda con el nombre definitivo
chmod 600 "$archivo"
echo "Respaldo: $archivo ($(du -h "$archivo" | cut -f1))"

find "$destino" -name 'nalu-*.dump' -type f -mtime "+$retencion" -print -delete | sed 's/^/Eliminado por retención: /'
