package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.VueloDTO;
import java.util.List;

public interface VueloService {

    List<VueloDTO> buscarVuelos(String origen, String destino, String fecha, int pasajeros);
}
