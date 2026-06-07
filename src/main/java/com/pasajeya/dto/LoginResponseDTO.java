package com.pasajeya.dto;

public record LoginResponseDTO(
        String token,
        String nombre,
        String rol
) {}
