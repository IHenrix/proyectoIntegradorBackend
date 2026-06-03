package com.pasajeya.service;

import com.pasajeya.dto.VueloDTO;
import java.util.List;

public interface VueloService {

    List<VueloDTO> buscarVuelos(String origen, String destino, String fecha, int pasajeros);
}
