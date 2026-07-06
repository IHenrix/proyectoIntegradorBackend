package pe.edu.utp.pasajeya.app.dto;

import java.math.BigDecimal;

public record AdminReporteResumenDTO(
        /** (suscripcionesActivas / usuariosTotal) * 100, redondeado. */
        BigDecimal tasaConversionPremium,
        /** ingresosTotales / suscripcionesActivas (0 si no hay ninguna activa). */
        BigDecimal ingresoPromedioPorSuscripcion,
        /** Ruta con más registros de historial de precios, formato "ORI-DES". */
        String rutaMasConsultada,
        long usuariosNuevosMesActual,
        long usuariosNuevosMesAnterior,
        BigDecimal ingresosMesActual,
        BigDecimal ingresosMesAnterior,
        long suscripcionesNuevasMesActual,
        long suscripcionesNuevasMesAnterior
) {}
