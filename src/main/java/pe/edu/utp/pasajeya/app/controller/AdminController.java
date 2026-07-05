package pe.edu.utp.pasajeya.app.controller;

import pe.edu.utp.pasajeya.app.dto.*;
import pe.edu.utp.pasajeya.app.service.AdminDashboardService;
import pe.edu.utp.pasajeya.app.service.AdminHistorialPrecioExcelService;
import pe.edu.utp.pasajeya.app.service.AdminHistorialPrecioService;
import pe.edu.utp.pasajeya.app.service.AdminSuscripcionService;
import pe.edu.utp.pasajeya.app.service.AdminUsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Panel de administración: dashboard de métricas, gestión de usuarios,
 * historial de precios y visión global de suscripciones/pagos. Todo el
 * controller exige rol ADMIN real (verificado vía el claim firmado del JWT,
 * no algo que el cliente pueda simular editando localStorage).
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminDashboardService dashboardService;
    private final AdminUsuarioService usuarioService;
    private final AdminHistorialPrecioService historialService;
    private final AdminHistorialPrecioExcelService historialExcelService;
    private final AdminSuscripcionService suscripcionService;

    public AdminController(AdminDashboardService dashboardService,
                            AdminUsuarioService usuarioService,
                            AdminHistorialPrecioService historialService,
                            AdminHistorialPrecioExcelService historialExcelService,
                            AdminSuscripcionService suscripcionService) {
        this.dashboardService = dashboardService;
        this.usuarioService = usuarioService;
        this.historialService = historialService;
        this.historialExcelService = historialExcelService;
        this.suscripcionService = suscripcionService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardDTO> dashboard() {
        return ResponseEntity.ok(dashboardService.obtenerMetricas());
    }

    // ── Usuarios ─────────────────────────────────────────────────────────

    @GetMapping("/usuarios")
    public ResponseEntity<List<AdminUsuarioListadoDTO>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @GetMapping("/usuarios/{id}")
    public ResponseEntity<AdminUsuarioDetalleDTO> detalleUsuario(@PathVariable Integer id) {
        return ResponseEntity.ok(usuarioService.obtenerDetalle(id));
    }

    @PatchMapping("/usuarios/{id}/rol")
    public ResponseEntity<AdminUsuarioListadoDTO> cambiarRol(
            @PathVariable Integer id,
            @RequestBody @Valid CambiarRolRequestDTO dto,
            Authentication auth) {
        return ResponseEntity.ok(usuarioService.cambiarRol(id, dto.rol(), auth.getName()));
    }

    @PatchMapping("/usuarios/{id}/activo")
    public ResponseEntity<AdminUsuarioListadoDTO> cambiarActivo(
            @PathVariable Integer id,
            @RequestParam boolean activo,
            Authentication auth) {
        return ResponseEntity.ok(usuarioService.cambiarEstadoActivo(id, activo, auth.getName()));
    }

    // ── Historial de precios ─────────────────────────────────────────────

    @GetMapping("/historial-precios")
    public ResponseEntity<List<AdminHistorialPrecioDTO>> historialPrecios(
            @RequestParam(required = false) Integer idVuelo,
            @RequestParam(required = false) String origen,
            @RequestParam(required = false) String destino,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        return ResponseEntity.ok(historialService.buscar(
                idVuelo, origen, destino, parseFechaInicio(desde), parseFechaFin(hasta)));
    }

    @GetMapping("/historial-precios/exportar")
    public ResponseEntity<byte[]> exportarHistorialPrecios(
            @RequestParam(required = false) Integer idVuelo,
            @RequestParam(required = false) String origen,
            @RequestParam(required = false) String destino,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        try {
            List<AdminHistorialPrecioDTO> filas = historialService.buscar(
                    idVuelo, origen, destino, parseFechaInicio(desde), parseFechaFin(hasta));
            byte[] excel = historialExcelService.generarExcel(filas);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=historial-precios.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excel);
        } catch (Exception e) {
            throw new RuntimeException("Error generando Excel: " + e.getMessage());
        }
    }

    // ── Suscripciones / Pagos ────────────────────────────────────────────

    @GetMapping("/suscripciones")
    public ResponseEntity<List<AdminSuscripcionDTO>> suscripciones() {
        return ResponseEntity.ok(suscripcionService.listarSuscripciones());
    }

    @GetMapping("/pagos")
    public ResponseEntity<List<AdminPagoDTO>> pagos() {
        return ResponseEntity.ok(suscripcionService.listarPagos());
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private LocalDateTime parseFechaInicio(String fecha) {
        return fecha == null || fecha.isBlank() ? null : LocalDate.parse(fecha).atStartOfDay();
    }

    private LocalDateTime parseFechaFin(String fecha) {
        return fecha == null || fecha.isBlank() ? null : LocalDate.parse(fecha).atTime(23, 59, 59);
    }
}
