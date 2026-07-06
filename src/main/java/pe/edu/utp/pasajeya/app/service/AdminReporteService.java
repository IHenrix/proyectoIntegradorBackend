package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.AdminReporteResumenDTO;

public interface AdminReporteService {

    /** KPIs y comparativo mes actual vs anterior, para la pestaña de Reportes del panel admin. */
    AdminReporteResumenDTO obtenerResumen();
}
