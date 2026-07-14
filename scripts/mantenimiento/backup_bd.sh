#!/bin/bash
# =============================================================================
# backup_bd.sh — Backup diario de la base de datos PostgreSQL de PasajeYa
# -----------------------------------------------------------------------------
# Copia comprimida de la BD con verificacion de integridad y rotacion a 7 dias.
# Es la "Copia 1" (local) de la estrategia 3-2-1. El espejo a un segundo medio
# (rsync) y la copia fuera del sitio (rclone a la nube) se programan aparte en
# el crontab, tras este backup.
#
# Uso:      /opt/pasajeya/scripts/backup_bd.sh
# Cron:     0 2 * * *  /opt/pasajeya/scripts/backup_bd.sh >> /var/log/pasajeya/backup.log 2>&1
# Permisos: chmod 750 backup_bd.sh
#
# Requiere las variables de conexion en el entorno (las mismas que usa Spring
# Boot en produccion): DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, PGPASSWORD.
# =============================================================================
set -euo pipefail   # aborta al primer error, variable no definida o fallo en un pipe

# --- Configuracion (con valores por defecto de desarrollo local) -------------
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-pasajeya}"
DB_USER="${DB_USERNAME:-postgres}"
DESTINO="${BACKUP_DIR:-/backups/pasajeya}"
RETENCION_DIAS=7

FECHA=$(date +%F)                       # 2026-07-13 (ordenable alfabeticamente)
ARCHIVO="$DESTINO/pasajeya_$FECHA.sql.gz"

mkdir -p "$DESTINO"

# --- 1) Volcar la BD comprimida (sin bloquear la aplicacion) -----------------
# pg_dump es el equivalente de mysqldump para PostgreSQL.
pg_dump --host="$DB_HOST" --port="$DB_PORT" --username="$DB_USER" \
        --no-owner --no-privileges "$DB_NAME" | gzip > "$ARCHIVO"

# --- 2) Verificar que el archivo existe y NO esta vacio (leccion GitLab) ------
[ -s "$ARCHIVO" ] || { echo "$(date '+%F %T') ERROR: backup vacio: $ARCHIVO"; exit 1; }

# --- 3) Rotacion: conservar solo los ultimos 7 dias --------------------------
find "$DESTINO" -name "pasajeya_*.sql.gz" -mtime +"$RETENCION_DIAS" -delete

TAMANO=$(du -h "$ARCHIVO" | cut -f1)
echo "$(date '+%F %T') Backup OK: $ARCHIVO ($TAMANO)"
