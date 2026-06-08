package pe.edu.utp.pasajeya.app.service.impl;

import pe.edu.utp.pasajeya.app.dto.AeropuertoDTO;
import pe.edu.utp.pasajeya.app.model.Aeropuerto;
import pe.edu.utp.pasajeya.app.repository.AeropuertoRepository;
import pe.edu.utp.pasajeya.app.service.AeropuertoService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class AeropuertoServiceImpl implements AeropuertoService {

    private static final Map<String, String> PAIS_NOMBRES = Map.of("PE", "Perú");

    private final AeropuertoRepository repo;

    public AeropuertoServiceImpl(AeropuertoRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<AeropuertoDTO> listar() {
        return repo.findAll().stream()
                .map(this::toDto)
                .sorted(Comparator.comparing(AeropuertoDTO::ciudad))
                .toList();
    }

    private AeropuertoDTO toDto(Aeropuerto aeropuerto) {
        String codigoPais = aeropuerto.getPais() != null ? aeropuerto.getPais().trim() : "PE";
        return new AeropuertoDTO(
                aeropuerto.getCodigo().trim(),
                aeropuerto.getCiudad(),
                aeropuerto.getNombre(),
                PAIS_NOMBRES.getOrDefault(codigoPais, "Perú")
        );
    }
}
