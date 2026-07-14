# =============================================================================
# healthcheck.ps1 - Health check con autorreparacion en Windows
# -----------------------------------------------------------------------------
# Version PowerShell de healthcheck.sh. Consulta /actuator/health y reinicia el
# servicio si no responde 200. Se programa cada 15 minutos con Task Scheduler:
#   schtasks /Create /TN "PasajeYa\HealthCheck" `
#     /TR "powershell -ExecutionPolicy Bypass -File C:\pasajeya\scripts\healthcheck.ps1" `
#     /SC MINUTE /MO 15 /F
# =============================================================================
$Url      = if ($env:HEALTH_URL) { $env:HEALTH_URL } else { "http://localhost:8080/actuator/health" }
$Log      = "C:\logs\pasajeya\healthcheck.log"
$Servicio = "pasajeya-backend"

New-Item -ItemType Directory -Force -Path (Split-Path $Log) | Out-Null

try {
    $resp = Invoke-WebRequest -Uri $Url -TimeoutSec 10 -UseBasicParsing
    $codigo = $resp.StatusCode
} catch {
    $codigo = 0
}

if ($codigo -ne 200) {
    Restart-Service -Name $Servicio -ErrorAction SilentlyContinue
    "$(Get-Date -Format 'u') App NO responde (HTTP $codigo) -> reiniciada" | Add-Content $Log
} else {
    "$(Get-Date -Format 'u') OK (HTTP 200)" | Add-Content $Log
}
