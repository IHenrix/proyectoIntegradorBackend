# Scripts de mantenimiento — PasajeYa

Mantenimiento **preventivo automatizado** del proyecto (Semana 17 — Unidad 4).
Se conecta con el Plan de Monitoreo de la Semana 16: el monitoreo **detecta**,
el mantenimiento **actúa**.

## Contenido

| Script | Qué hace | Frecuencia (cron) |
|---|---|---|
| `healthcheck.sh` | Consulta `/actuator/health` y reinicia el backend si no responde 200 | `*/15 * * * *` (cada 15 min) |
| `backup_bd.sh` | `pg_dump` comprimido de PostgreSQL + verificación + rotación 7 días (Copia 1 del 3-2-1) | `0 2 * * *` (diario 2 a.m.) |
| `limpieza.sh` | Borra logs > 30 días **solo si** el disco supera el 85% (mismo umbral que Grafana) | `30 3 * * 0` (domingos 3:30 a.m.) |
| `probar_restauracion.sh` | Restaura el último backup en una BD temporal y cuenta registros ("un backup no probado no existe") | `0 5 1 * *` (día 1 de cada mes) |
| `crontab.txt` | Calendario completo con las 6 líneas de cron (incluye espejo `rsync` y nube `rclone`) | — |

## Instalación en un servidor Linux

```bash
# 1) Copiar los scripts y darles permisos mínimos
sudo mkdir -p /opt/pasajeya/scripts /var/log/pasajeya /backups/pasajeya
sudo cp *.sh /opt/pasajeya/scripts/
sudo chmod 750 /opt/pasajeya/scripts/*.sh

# 2) Probar cada script A MANO antes de programarlo
/opt/pasajeya/scripts/healthcheck.sh

# 3) Instalar el calendario
crontab crontab.txt
crontab -l   # verificar
```

## Estrategia 3-2-1 de backups

- **3 copias**: original + backup local (`backup_bd.sh`) + espejo (`rsync`).
- **2 medios**: disco del servidor + disco secundario (`/mnt/disco2`).
- **1 fuera del sitio**: copia a la nube (`rclone`), sobrevive a incendio/robo/ransomware.

## Nota sobre el despliegue en Railway (PaaS)

En producción, PasajeYa corre en **Railway**, un PaaS gestionado que **no expone
el `crontab` del sistema operativo**. Por eso:

- El backup gestionado lo provee Railway a nivel de servicio de PostgreSQL.
- El equivalente de *cron dentro de la app* ya existe en el proyecto vía
  **`@Scheduled`** de Spring Boot (job de captura de historial de precios y
  autorenovación de suscripciones).
- Estos scripts `.sh` quedan **versionados en Git** como evidencia del plan de
  mantenimiento y son directamente ejecutables en un despliegue *self-hosted*
  (VPS/servidor Linux propio).
