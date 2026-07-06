package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.AdminDashboardDTO;
import pe.edu.utp.pasajeya.app.dto.AdminPrecioRutaSemanaDTO;

import java.util.List;

public interface AdminDashboardService {

    /** Métricas agregadas del sistema completo, solo para el panel admin. */
    AdminDashboardDTO obtenerMetricas();

    /**
     * Serie de precio promedio semanal para las rutas con más volumen de
     * historial (últimos 90 días) — vive separado del DTO principal porque es
     * una serie más pesada y con estructura distinta (ruta × semana).
     */
    List<AdminPrecioRutaSemanaDTO> obtenerPreciosPorRuta();
}
