package pe.edu.utp.pasajeya.app.controller;

import jakarta.validation.Valid;
import pe.edu.utp.pasajeya.app.dto.AlertaDTO;
import pe.edu.utp.pasajeya.app.dto.CrearAlertaRequestDTO;
import pe.edu.utp.pasajeya.app.service.AlertaService;
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

    public AlertaController(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    @GetMapping
    public ResponseEntity<List<AlertaDTO>> listar(Authentication auth) {
        return ResponseEntity.ok(alertaService.listar(auth.getName()));
    }

    @PostMapping
    public ResponseEntity<AlertaDTO> crear(Authentication auth,
                                           @RequestBody @Valid CrearAlertaRequestDTO request) {
        return ResponseEntity.ok(alertaService.crear(auth.getName(), request));
    }

    @PatchMapping("/{id}/pausar")
    public ResponseEntity<AlertaDTO> pausar(Authentication auth, @PathVariable Integer id) {
        return ResponseEntity.ok(alertaService.pausar(auth.getName(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(Authentication auth, @PathVariable Integer id) {
        alertaService.eliminar(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
