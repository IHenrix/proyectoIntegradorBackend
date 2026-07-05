package pe.edu.utp.pasajeya.app.dto;

import jakarta.validation.constraints.NotBlank;

public record CambiarRolRequestDTO(@NotBlank String rol) {}
