package pe.edu.utp.pasajeya.app.dto;

public record LoginResponseDTO(
        String token,
        String nombre,
        String rol
) {}
