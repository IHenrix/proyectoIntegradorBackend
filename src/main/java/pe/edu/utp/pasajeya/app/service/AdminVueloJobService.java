package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.AdminJobEstadoDTO;

public interface AdminVueloJobService {

    AdminJobEstadoDTO obtenerEstado();

    /** Dispara el job real de captura de precios/alertas bajo demanda. */
    void ejecutarAhora();
}
