#!/usr/bin/env bash
# Restaura un respaldo de Nalu (RNF12) en una base de datos NUEVA del contenedor, sin tocar la actual.
#
#   scripts/restaurar.sh respaldos/nalu-20261109-031500.dump [nombre_bd_destino]
#
# Para usar la base restaurada, apunta DB_URL a ella. Sobrescribir la base en uso es una decisión
# manual a propósito: este script nunca borra datos.
set -euo pipefail

archivo=${1:?"Uso: scripts/restaurar.sh <archivo.dump> [base_destino]"}
destino=${2:-nalu_restaurada_$(date +%Y%m%d%H%M%S)}
contenedor=${NALU_CONTENEDOR_BD:-nalu-postgres}
[[ $destino =~ ^[a-z_][a-z0-9_]*$ ]] || { echo "Nombre de base no válido: $destino" >&2; exit 1; }

inicio=$(date +%s)
docker exec "$contenedor" sh -c "createdb -U \"\$POSTGRES_USER\" $destino"
docker exec -i "$contenedor" sh -c "pg_restore -U \"\$POSTGRES_USER\" -d $destino --no-owner --exit-on-error" < "$archivo"
echo "Restaurado en la base '$destino' en $(( $(date +%s) - inicio )) s"
