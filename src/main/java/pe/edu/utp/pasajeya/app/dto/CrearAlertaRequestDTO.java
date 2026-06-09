package pe.edu.utp.pasajeya.app.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CrearAlertaRequestDTO(
        Long tarifaId,
        String origen,
        String destino,
        String fecha,
        String tipoTarifa,
        @NotNull
        @DecimalMin("1.00") Double precioObjetivo,
        @NotBlank
        @Pattern(regexp = "^(\\+?51)?9[0-9]{8}$", message = "El telefono debe ser un celular peruano valido")
        String telefono
) {}
