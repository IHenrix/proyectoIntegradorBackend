package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.AdminHistorialPrecioDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminHistorialPrecioService {

    List<AdminHistorialPrecioDTO> buscar(
            Integer idVuelo, String origen, String destino,
            LocalDateTime desde, LocalDateTime hasta);
}
