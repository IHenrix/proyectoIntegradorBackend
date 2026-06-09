package pe.edu.utp.pasajeya.app.dto;

public record ActualizarPerfilRequestDTO(
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String genero,
        String telefono,
        String fechaNacimiento,
        String tipoDocumento,
        String nroDocumento,
        String passwordActual,
        String passwordNuevo
) {}
