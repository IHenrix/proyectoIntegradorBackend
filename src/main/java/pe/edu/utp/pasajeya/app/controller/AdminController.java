package pe.edu.utp.pasajeya.app.controller;

import pe.edu.utp.pasajeya.app.dto.*;
import pe.edu.utp.pasajeya.app.service.AdminDashboardPdfService;
import pe.edu.utp.pasajeya.app.service.AdminDashboardService;
import pe.edu.utp.pasajeya.app.service.AdminHistorialPrecioExcelService;
import pe.edu.utp.pasajeya.app.service.AdminHistorialPrecioService;
import pe.edu.utp.pasajeya.app.service.AdminReporteService;
import pe.edu.utp.pasajeya.app.service.AdminSuscripcionExcelService;
import pe.edu.utp.pasajeya.app.service.AdminSuscripcionService;
import pe.edu.utp.pasajeya.app.service.AdminUsuarioExcelService;
import pe.edu.utp.pasajeya.app.service.AdminUsuarioService;
import pe.edu.utp.pasajeya.app.service.AdminVueloJobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
    private final AdminReporteService reporteService;
    private final AdminUsuarioExcelService usuarioExcelService;
    private final AdminSuscripcionExcelService suscripcionExcelService;
    private final AdminDashboardPdfService dashboardPdfService;
    private final AdminVueloJobService vueloJobService;

    public AdminController(AdminDashboardService dashboardService,
                            AdminUsuarioService usuarioService,
                            AdminHistorialPrecioService historialService,
                            AdminHistorialPrecioExcelService historialExcelService,
                            AdminSuscripcionService suscripcionService,
                            AdminReporteService reporteService,
                            AdminUsuarioExcelService usuarioExcelService,
                            AdminSuscripcionExcelService suscripcionExcelService,
                            AdminDashboardPdfService dashboardPdfService,
                            AdminVueloJobService vueloJobService) {
        this.dashboardService = dashboardService;
        this.usuarioService = usuarioService;
        this.historialService = historialService;
        this.historialExcelService = historialExcelService;
        this.suscripcionService = suscripcionService;
        this.reporteService = reporteService;
        this.usuarioExcelService = usuarioExcelService;
        this.suscripcionExcelService = suscripcionExcelService;
        this.dashboardPdfService = dashboardPdfService;
        this.vueloJobService = vueloJobService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardDTO> dashboard() {
        return ResponseEntity.ok(dashboardService.obtenerMetricas());
    }

    @GetMapping("/dashboard/precios-ruta")
    public ResponseEntity<List<AdminPrecioRutaSemanaDTO>> preciosPorRuta() {
        return ResponseEntity.ok(dashboardService.obtenerPreciosPorRuta());
    }

    // ── Usuarios ─────────────────────────────────────────────────────────

    @GetMapping("/usuarios")
    public ResponseEntity<PaginaDTO<AdminUsuarioListadoDTO>> listarUsuarios(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(usuarioService.listar(pagina, tamano, q));
    }

    @GetMapping("/usuarios/{id}")
    public ResponseEntity<AdminUsuarioDetalleDTO> detalleUsuario(@PathVariable Integer id) {
        return ResponseEntity.ok(usuarioService.obtenerDetalle(id));
    }

    @PostMapping("/usuarios")
    public ResponseEntity<AdminUsuarioListadoDTO> crearUsuario(@RequestBody @Valid CrearUsuarioRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(dto));
    }

    @PutMapping("/usuarios/{id}")
    public ResponseEntity<AdminUsuarioListadoDTO> editarUsuario(
            @PathVariable Integer id, @RequestBody @Valid EditarUsuarioRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.editar(id, dto));
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
    public ResponseEntity<PaginaDTO<AdminHistorialPrecioDTO>> historialPrecios(
            @RequestParam(required = false) Integer idVuelo,
            @RequestParam(required = false) String origen,
            @RequestParam(required = false) String destino,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano) {
        return ResponseEntity.ok(historialService.buscarPaginado(
                idVuelo, origen, destino, parseFechaInicio(desde), parseFechaFin(hasta), q, pagina, tamano));
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
    public ResponseEntity<PaginaDTO<AdminSuscripcionDTO>> suscripciones(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano) {
        return ResponseEntity.ok(suscripcionService.listarSuscripcionesPaginado(q, pagina, tamano));
    }

    @GetMapping("/pagos")
    public ResponseEntity<List<AdminPagoDTO>> pagos() {
        return ResponseEntity.ok(suscripcionService.listarPagos());
    }

    // ── Reportes ─────────────────────────────────────────────────────────

    @GetMapping("/reportes/resumen")
    public ResponseEntity<AdminReporteResumenDTO> reporteResumen() {
        return ResponseEntity.ok(reporteService.obtenerResumen());
    }

    @GetMapping("/reportes/exportar-pdf")
    public ResponseEntity<byte[]> exportarReportePdf() {
        try {
            byte[] pdf = dashboardPdfService.generar(dashboardService.obtenerMetricas(), reporteService.obtenerResumen());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte-ejecutivo.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF: " + e.getMessage());
        }
    }

    // ── Exportación ──────────────────────────────────────────────────────

    @GetMapping("/usuarios/exportar")
    public ResponseEntity<byte[]> exportarUsuarios() {
        try {
            byte[] excel = usuarioExcelService.generarExcel(usuarioService.listarTodos());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=usuarios.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excel);
        } catch (Exception e) {
            throw new RuntimeException("Error generando Excel: " + e.getMessage());
        }
    }

    @GetMapping("/suscripciones/exportar")
    public ResponseEntity<byte[]> exportarSuscripciones() {
        try {
            byte[] excel = suscripcionExcelService.generarExcel(suscripcionService.listarSuscripcionesTodas());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=suscripciones.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excel);
        } catch (Exception e) {
            throw new RuntimeException("Error generando Excel: " + e.getMessage());
        }
    }

    // ── Simulación de vuelos (job de captura + generador de datos de prueba) ──

    @GetMapping("/vuelos-job/estado")
    public ResponseEntity<AdminJobEstadoDTO> estadoVuelosJob() {
        return ResponseEntity.ok(vueloJobService.obtenerEstado());
    }

    @PostMapping("/vuelos-job/ejecutar-ahora")
    public ResponseEntity<AdminJobEstadoDTO> ejecutarVuelosJobAhora() {
        vueloJobService.ejecutarAhora();
        return ResponseEntity.ok(vueloJobService.obtenerEstado());
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private LocalDateTime parseFechaInicio(String fecha) {
        return fecha == null || fecha.isBlank() ? null : LocalDate.parse(fecha).atStartOfDay();
    }

    private LocalDateTime parseFechaFin(String fecha) {
        return fecha == null || fecha.isBlank() ? null : LocalDate.parse(fecha).atTime(23, 59, 59);
    }
}
