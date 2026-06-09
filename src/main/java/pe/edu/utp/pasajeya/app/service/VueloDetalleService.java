package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.VueloDetalleDTO;

public interface VueloDetalleService {

    VueloDetalleDTO obtenerDetalle(Long tarifaId);
}
