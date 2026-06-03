package com.pasajeya.dto;

/**
 * DTO que expone la información del Singleton DatabaseConnection.
 */
public record SistemaInfoDTO(
        String patron,
        String instancia,
        String url,
        String nombreBD,
        int    puerto,
        String estado
) {}
