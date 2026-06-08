package pe.edu.utp.pasajeya.app.controller;

import pe.edu.utp.pasajeya.app.dto.AeropuertoDTO;
import pe.edu.utp.pasajeya.app.repository.AeropuertoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/aeropuertos")
public class AeropuertoController {

    private static final Map<String, String> PAIS_NOMBRES = Map.of("PE", "Perú");

    private final AeropuertoRepository repo;

    public AeropuertoController(AeropuertoRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public ResponseEntity<List<AeropuertoDTO>> listar() {
        List<AeropuertoDTO> result = repo.findAll().stream()
                .map(a -> new AeropuertoDTO(
                        a.getCodigo().trim(),
                        a.getCiudad(),
                        a.getNombre(),
                        PAIS_NOMBRES.getOrDefault(a.getPais() != null ? a.getPais().trim() : "PE", "Perú")))
                .sorted(Comparator.comparing(AeropuertoDTO::ciudad))
                .toList();
        return ResponseEntity.ok(result);
    }
}
