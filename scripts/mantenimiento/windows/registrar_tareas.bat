@echo off
REM ============================================================================
REM registrar_tareas.bat - Registra el calendario de mantenimiento de PasajeYa
REM                        en el Programador de tareas de Windows (Task Scheduler)
REM Equivalente Windows del crontab.txt de Linux. Ejecutar como Administrador.
REM ============================================================================
set BASE=C:\pasajeya\scripts\windows
set PS=powershell -ExecutionPolicy Bypass -File

REM 1) Health check + autorreparacion - cada 15 minutos
schtasks /Create /TN "PasajeYa\HealthCheck" ^
  /TR "%PS% %BASE%\healthcheck.ps1" ^
  /SC MINUTE /MO 15 /F

REM 2) Backup diario de la BD (Copia 1: local) - 2:00 a.m.
schtasks /Create /TN "PasajeYa\BackupDiario" ^
  /TR "%PS% %BASE%\backup_bd.ps1" ^
  /SC DAILY /ST 02:00 /RL HIGHEST /F

REM 3) Espejo a disco secundario (Copia 2) - 2:20 a.m.
schtasks /Create /TN "PasajeYa\EspejoDisco2" ^
  /TR "robocopy C:\backups\pasajeya D:\backups\pasajeya /MIR" ^
  /SC DAILY /ST 02:20 /F

REM 4) Limpieza condicional de logs - domingos 3:30 a.m.
schtasks /Create /TN "PasajeYa\Limpieza" ^
  /TR "%PS% %BASE%\limpieza.ps1" ^
  /SC WEEKLY /D SUN /ST 03:30 /F

REM 5) Restauracion de PRUEBA - dia 1 de cada mes, 5:00 a.m.
schtasks /Create /TN "PasajeYa\RestauracionPrueba" ^
  /TR "%PS% %BASE%\probar_restauracion.ps1" ^
  /SC MONTHLY /D 1 /ST 05:00 /F

echo.
echo Tareas registradas. Listar con:  schtasks /Query /TN "PasajeYa\*" /FO TABLE
