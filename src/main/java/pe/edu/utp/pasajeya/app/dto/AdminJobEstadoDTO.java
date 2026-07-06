package pe.edu.utp.pasajeya.app.dto;

public record AdminJobEstadoDTO(
        /** ISO string, o null si el job no ha corrido todavía en esta instancia del backend. */
        String ultimaEjecucion,
        /** ISO string estimado (ultimaEjecucion + tasaCapturaMs), o null si nunca corrió. */
        String proximaEjecucionEstimada,
        long totalTarifas,
        long totalVuelos,
        long totalHistorial,
        long tasaCapturaMs
) {}
