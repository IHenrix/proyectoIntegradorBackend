package pe.edu.utp.pasajeya.app.dto;

import java.math.BigDecimal;

public record AdminPagoDTO(
        Integer id,
        String emailUsuario,
        String nombreUsuario,
        BigDecimal monto,
        String moneda,
        String metodo,
        String estado,
        String refInterna,
        String fechaPago
) {}
