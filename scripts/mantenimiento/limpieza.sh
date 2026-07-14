#!/bin/bash
# =============================================================================
# limpieza.sh — Limpieza condicional de logs y temporales de PasajeYa
# -----------------------------------------------------------------------------
# Mantenimiento PREVENTIVO: solo borra si el disco supera el umbral. El 85% es
# el MISMO umbral de saturacion que definimos como alerta en Grafana en la
# semana 16 (Plan de Monitoreo): monitoreo detecta, mantenimiento actua.
#
# Uso:      /opt/pasajeya/scripts/limpieza.sh
# Cron:     30 3 * * 0  /opt/pasajeya/scripts/limpieza.sh >> /var/log/pasajeya/limpieza.log 2>&1
# Permisos: chmod 750 limpieza.sh
# =============================================================================
set -euo pipefail

LOG_DIR="${LOG_DIR:-/var/log/pasajeya}"
UMBRAL=85               # % de uso de disco que dispara la limpieza
RETENCION_LOGS=30       # dias

# --- ¿Cuanto disco esta usado? (solo el numero, sin el signo %) --------------
USO=$(df --output=pcent / | tr -dc '0-9')

if [ "$USO" -gt "$UMBRAL" ]; then
    # Borrar logs de la aplicacion con mas de 30 dias
    find "$LOG_DIR" -name "*.log" -mtime +"$RETENCION_LOGS" -delete
    # Limpiar imagenes Docker huerfanas (si el host las usa)
    command -v docker >/dev/null 2>&1 && docker image prune -f >/dev/null 2>&1 || true
    echo "$(date '+%F %T') Limpieza ejecutada (disco al ${USO}%, umbral ${UMBRAL}%)"
else
    echo "$(date '+%F %T') Sin accion (disco al ${USO}%, por debajo del ${UMBRAL}%)"
fi
