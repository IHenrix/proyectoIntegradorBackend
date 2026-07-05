package pe.edu.utp.pasajeya.app.dto;

import java.math.BigDecimal;

public record AdminSuscripcionDTO(
        Integer id,
        String emailUsuario,
        String nombreUsuario,
        String planNombre,
        String tipoPlan,
        BigDecimal monto,
        String fechaInicio,
        String fechaFin,
        String estado,
        String metodoPago,
        Boolean autoRenovar
) {}
