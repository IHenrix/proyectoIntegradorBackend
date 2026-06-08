package pe.edu.utp.pasajeya.app.controller;

import pe.edu.utp.pasajeya.app.dto.AeropuertoDTO;
import pe.edu.utp.pasajeya.app.service.AeropuertoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/aeropuertos")
public class AeropuertoController {

    private final AeropuertoService aeropuertoService;

    public AeropuertoController(AeropuertoService aeropuertoService) {
        this.aeropuertoService = aeropuertoService;
    }

    @GetMapping
    public ResponseEntity<List<AeropuertoDTO>> listar() {
        return ResponseEntity.ok(aeropuertoService.listar());
    }
}
