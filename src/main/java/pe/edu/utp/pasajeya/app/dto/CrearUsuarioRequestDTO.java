package pe.edu.utp.pasajeya.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A diferencia de RegistroRequestDTO (registro público, rol siempre forzado a
 * usuario_free), este DTO lo usa el admin para crear cuentas y sí permite
 * elegir el rol libremente.
 */
public record CrearUsuarioRequestDTO(
        @NotBlank String nombre,
        @NotBlank String apellidoPaterno,
        String apellidoMaterno,
        String genero,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        String telefono,
        String fechaNacimiento,
        Integer tipoDocumentoId,
        String nroDocumento,
        @NotBlank String rol
) {}
