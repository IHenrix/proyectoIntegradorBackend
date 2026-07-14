#!/bin/bash
# =============================================================================
# healthcheck.sh — Verificacion de salud con autorreparacion de PasajeYa
# -----------------------------------------------------------------------------
# Consulta el endpoint /actuator/health (Spring Boot Actuator, agregado en la
# semana 16). Si el backend no responde 200, lo reinicia. El log alimenta a
# Grafana: monitoreo (detectar) + mantenimiento (actuar) se retroalimentan.
#
# Uso:      /opt/pasajeya/scripts/healthcheck.sh
# Cron:     */15 * * * *  /opt/pasajeya/scripts/healthcheck.sh
# Permisos: chmod 750 healthcheck.sh
# =============================================================================
set -euo pipefail

URL="${HEALTH_URL:-http://localhost:8080/actuator/health}"
LOG="${HEALTH_LOG:-/var/log/pasajeya/healthcheck.log}"
SERVICIO="${APP_SERVICE:-pasajeya-backend}"

mkdir -p "$(dirname "$LOG")"

# --- Pedir solo el codigo HTTP (sin descargar el cuerpo), timeout 10s --------
CODIGO=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 "$URL" || echo "000")

if [ "$CODIGO" -ne 200 ]; then
    # Reinicio automatico del servicio (systemd). En un host con Docker seria:
    #   docker restart "$SERVICIO"
    systemctl restart "$SERVICIO" 2>/dev/null || true
    echo "$(date '+%F %T') App NO responde (HTTP $CODIGO) -> reiniciada" >> "$LOG"
else
    echo "$(date '+%F %T') OK (HTTP 200)" >> "$LOG"
fi
