package pe.edu.utp.pasajeya.app.dto;

import java.math.BigDecimal;
import java.util.Map;

public record AdminDashboardDTO(
        Map<String, Long> usuariosPorRol,
        long usuariosActivos,
        long usuariosInactivos,
        BigDecimal ingresosTotales,
        long alertasActivas,
        long suscripcionesActivas,
        long suscripcionesVencidas,
        long suscripcionesCanceladas,
        /** Clave "YYYY-MM", últimos 12 meses con pagos aprobados. Para el gráfico de ingresos mensuales. */
        Map<String, BigDecimal> ingresosPorMes,
        /** Conteo de alertas activas agrupado por nombre de aerolínea del vuelo asociado. */
        Map<String, Long> alertasPorAerolinea
) {}
