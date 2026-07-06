package pe.edu.utp.pasajeya.app.controller;

import jakarta.validation.Valid;
import pe.edu.utp.pasajeya.app.dto.AlertaDTO;
import pe.edu.utp.pasajeya.app.dto.CrearAlertaRequestDTO;
import pe.edu.utp.pasajeya.app.service.AlertaExcelService;
import pe.edu.utp.pasajeya.app.service.AlertaPdfService;
import pe.edu.utp.pasajeya.app.service.AlertaService;
import pe.edu.utp.pasajeya.app.repository.UsuarioRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alertas")
public class AlertaController {

    private final AlertaService alertaService;
    private final AlertaExcelService excelService;
    private final AlertaPdfService pdfService;
    private final UsuarioRepository usuarioRepo;

    public AlertaController(AlertaService alertaService,
                            AlertaExcelService excelService,
                            AlertaPdfService pdfService,
                            UsuarioRepository usuarioRepo) {
        this.alertaService = alertaService;
        this.excelService  = excelService;
        this.pdfService    = pdfService;
        this.usuarioRepo   = usuarioRepo;
    }

    @GetMapping
    public ResponseEntity<List<AlertaDTO>> listar(Authentication auth) {
        verificarNoAdmin(auth.getName());
        return ResponseEntity.ok(alertaService.listar(auth.getName()));
    }

    @PostMapping
    public ResponseEntity<AlertaDTO> crear(Authentication auth,
                                           @RequestBody @Valid CrearAlertaRequestDTO request) {
        verificarNoAdmin(auth.getName());
        return ResponseEntity.ok(alertaService.crear(auth.getName(), request));
    }

    @PatchMapping("/{id}/pausar")
    public ResponseEntity<AlertaDTO> pausar(Authentication auth, @PathVariable Integer id) {
        verificarNoAdmin(auth.getName());
        return ResponseEntity.ok(alertaService.pausar(auth.getName(), id));
    }

    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<AlertaDTO> reactivar(Authentication auth, @PathVariable Integer id) {
        verificarNoAdmin(auth.getName());
        return ResponseEntity.ok(alertaService.reactivar(auth.getName(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(Authentication auth, @PathVariable Integer id) {
        verificarNoAdmin(auth.getName());
        alertaService.eliminar(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reporte/excel")
    public ResponseEntity<byte[]> reporteExcel(Authentication auth) {
        verificarPremium(auth.getName());
        List<AlertaDTO> alertas = alertaService.listar(auth.getName());
        String nombre = resolverNombre(auth.getName());
        try {
            byte[] bytes = excelService.generar(alertas, nombre);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"alertas_pasajeyа.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Error generando Excel: " + e.getMessage());
        }
    }

    @GetMapping("/reporte/pdf")
    public ResponseEntity<byte[]> reportePdf(Authentication auth) {
        verificarPremium(auth.getName());
        List<AlertaDTO> alertas = alertaService.listar(auth.getName());
        String nombre = resolverNombre(auth.getName());
        try {
            byte[] bytes = pdfService.generar(alertas, nombre);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"alertas_pasajeyа.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF: " + e.getMessage());
        }
    }

    private void verificarNoAdmin(String email) {
        usuarioRepo.findByEmail(email).ifPresent(u -> {
            if ("admin".equals(u.getRol().getNombre())) {
                throw new RuntimeException("El administrador no gestiona alertas propias");
            }
        });
    }

    private void verificarPremium(String email) {
        usuarioRepo.findByEmail(email).ifPresent(u -> {
            String rol = u.getRol().getNombre();
            if (!"usuario_premium".equals(rol)) {
                throw new RuntimeException("REQUIERE_PREMIUM");
            }
        });
    }

    private String resolverNombre(String email) {
        return usuarioRepo.findByEmail(email)
                .map(u -> u.getPersona().getNombre() + " " + u.getPersona().getApellidoPaterno())
                .orElse(email);
    }
}
