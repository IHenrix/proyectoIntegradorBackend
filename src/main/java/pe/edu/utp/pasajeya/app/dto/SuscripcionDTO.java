package pe.edu.utp.pasajeya.app.dto;

import java.math.BigDecimal;

public record SuscripcionDTO(
        Integer    id,
        String     planNombre,
        String     tipoPlan,      // "mensual" | "anual"
        BigDecimal monto,
        String     fechaInicio,
        String     fechaFin,
        String     estado,
        String     metodoPago,
        String     refInterna,    // número visible al usuario, ej: "748291"
        Boolean    autoRenovar    // true solo si el método fue tarjeta
) {}
