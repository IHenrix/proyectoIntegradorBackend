package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.AdminDashboardDTO;

public interface AdminDashboardService {

    /** Métricas agregadas del sistema completo, solo para el panel admin. */
    AdminDashboardDTO obtenerMetricas();
}
