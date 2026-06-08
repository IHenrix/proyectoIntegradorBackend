package pe.edu.utp.pasajeya.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequestDTO(
        @NotBlank String nombre,
        @NotBlank String apellidoPaterno,
        String apellidoMaterno,
        String genero,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        String telefono,
        String fechaNacimiento,
        Integer tipoDocumentoId,
        String nroDocumento
) {}
