# =============================================================================
# backup_bd.ps1 - Backup de PostgreSQL en Windows (alternativa a backup_bd.sh)
# -----------------------------------------------------------------------------
# Version PowerShell del backup diario de PasajeYa. Se programa con el
# Programador de tareas de Windows (Task Scheduler), equivalente de cron.
#
#   Registrar:  schtasks /Create /TN "PasajeYa\BackupDiario" `
#                 /TR "powershell -ExecutionPolicy Bypass -File C:\pasajeya\scripts\backup_bd.ps1" `
#                 /SC DAILY /ST 02:00 /RL HIGHEST /F
# =============================================================================
$ErrorActionPreference = "Stop"          # aborta al primer error

$Fecha   = Get-Date -Format "yyyy-MM-dd" # 2026-07-13 (ordenable)
$Destino = if ($env:BACKUP_DIR) { $env:BACKUP_DIR } else { "C:\backups\pasajeya" }
$Bd      = if ($env:DB_NAME)    { $env:DB_NAME }    else { "pasajeya" }
$Archivo = Join-Path $Destino "pasajeya_$Fecha.sql"
$Zip     = "$Archivo.zip"

New-Item -ItemType Directory -Force -Path $Destino | Out-Null

# 1) Volcar la BD y comprimir
pg_dump --no-owner --no-privileges $Bd | Out-File -Encoding utf8 $Archivo
Compress-Archive -Path $Archivo -DestinationPath $Zip -Force
Remove-Item $Archivo

# 2) Verificar que el zip no salio vacio (leccion GitLab)
if ((Get-Item $Zip).Length -eq 0) { throw "Backup vacio: $Zip" }

# 3) Rotacion: borrar backups con mas de 7 dias
Get-ChildItem $Destino -Filter "pasajeya_*.zip" |
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-7) } |
    Remove-Item -Force

"$(Get-Date -Format 'u') Backup OK: $Zip"
