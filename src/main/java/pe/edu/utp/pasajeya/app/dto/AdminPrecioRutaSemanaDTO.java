package pe.edu.utp.pasajeya.app.dto;

import java.math.BigDecimal;

public record AdminPrecioRutaSemanaDTO(
        String ruta,
        String semana,
        BigDecimal precioPromedio
) {}
