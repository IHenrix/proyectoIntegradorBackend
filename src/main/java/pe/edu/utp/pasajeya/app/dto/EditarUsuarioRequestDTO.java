package pe.edu.utp.pasajeya.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * password es OPCIONAL: si viene null/blank, el servicio no toca el hash
 * existente. Si viene, se valida longitud mínima a mano en el servicio (igual
 * patrón que AuthServiceImpl.registro usa Preconditions.checkArgument), ya
 * que no se puede anotar @Size(min=8) directo en un campo que también debe
 * aceptar blank/null.
 */
public record EditarUsuarioRequestDTO(
        @NotBlank String nombre,
        @NotBlank String apellidoPaterno,
        String apellidoMaterno,
        String genero,
        @NotBlank @Email String email,
        String password,
        String telefono,
        String fechaNacimiento,
        Integer tipoDocumentoId,
        String nroDocumento,
        @NotBlank String rol,
        Boolean activo
) {}
