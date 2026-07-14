#!/bin/bash
# =============================================================================
# probar_restauracion.sh — Restauracion de PRUEBA mensual del backup de PasajeYa
# -----------------------------------------------------------------------------
# "Un backup no probado NO existe" (leccion GitLab 2017). Restaura el ultimo
# backup en una BD TEMPORAL y cuenta registros para confirmar que el .sql.gz
# es recuperable. NUNCA toca la base de datos de produccion.
#
# Uso:      /opt/pasajeya/scripts/probar_restauracion.sh
# Cron:     0 5 1 * *  /opt/pasajeya/scripts/probar_restauracion.sh >> /var/log/pasajeya/restauracion.log 2>&1
# Permisos: chmod 750 probar_restauracion.sh
# =============================================================================
set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USERNAME:-postgres}"
DESTINO="${BACKUP_DIR:-/backups/pasajeya}"
BD_PRUEBA="pasajeya_restore_test"

# --- 1) Ubicar el backup mas reciente ----------------------------------------
ULTIMO=$(find "$DESTINO" -name "pasajeya_*.sql.gz" -printf '%T@ %p\n' \
         | sort -nr | head -1 | cut -d' ' -f2-)
[ -n "$ULTIMO" ] || { echo "$(date '+%F %T') ERROR: no hay backups en $DESTINO"; exit 1; }

# --- 2) Crear una BD temporal limpia y restaurar sobre ella ------------------
psql --host="$DB_HOST" --port="$DB_PORT" --username="$DB_USER" -c \
     "DROP DATABASE IF EXISTS $BD_PRUEBA;" >/dev/null
psql --host="$DB_HOST" --port="$DB_PORT" --username="$DB_USER" -c \
     "CREATE DATABASE $BD_PRUEBA;" >/dev/null
gunzip -c "$ULTIMO" | psql --host="$DB_HOST" --port="$DB_PORT" \
     --username="$DB_USER" "$BD_PRUEBA" >/dev/null

# --- 3) Verificar que hay datos: contar usuarios y vuelos --------------------
USUARIOS=$(psql --host="$DB_HOST" --port="$DB_PORT" --username="$DB_USER" \
     -tAc "SELECT COUNT(*) FROM usuario;" "$BD_PRUEBA")
VUELOS=$(psql --host="$DB_HOST" --port="$DB_PORT" --username="$DB_USER" \
     -tAc "SELECT COUNT(*) FROM vuelo;" "$BD_PRUEBA")

# --- 4) Limpiar la BD temporal -----------------------------------------------
psql --host="$DB_HOST" --port="$DB_PORT" --username="$DB_USER" -c \
     "DROP DATABASE IF EXISTS $BD_PRUEBA;" >/dev/null

if [ "$USUARIOS" -gt 0 ] && [ "$VUELOS" -gt 0 ]; then
    echo "$(date '+%F %T') Restauracion OK desde $(basename "$ULTIMO") -> usuarios=$USUARIOS, vuelos=$VUELOS"
else
    echo "$(date '+%F %T') ALERTA: backup restaurado pero SIN datos (usuarios=$USUARIOS, vuelos=$VUELOS)"
    exit 1
fi
