package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.AlertaDTO;
import pe.edu.utp.pasajeya.app.dto.CrearAlertaRequestDTO;

import java.util.List;

public interface AlertaService {

    List<AlertaDTO> listar(String email);

    AlertaDTO crear(String email, CrearAlertaRequestDTO request);

    AlertaDTO pausar(String email, Integer id);

    AlertaDTO reactivar(String email, Integer id);

    void eliminar(String email, Integer id);

    void evaluarAlertasActivas();
}
