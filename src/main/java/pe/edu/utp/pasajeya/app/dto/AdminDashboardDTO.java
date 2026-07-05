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
        long suscripcionesCanceladas
) {}
