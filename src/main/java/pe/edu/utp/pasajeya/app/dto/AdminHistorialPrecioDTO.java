package pe.edu.utp.pasajeya.app.dto;

import java.math.BigDecimal;

public record AdminHistorialPrecioDTO(
        Integer idVuelo,
        String aerolinea,
        String origen,
        String destino,
        BigDecimal precio,
        String tipoTarifa,
        String fechaCaptura
) {}
