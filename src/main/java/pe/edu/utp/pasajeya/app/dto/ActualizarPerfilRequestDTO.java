package pe.edu.utp.pasajeya.app.dto;

public record ActualizarPerfilRequestDTO(
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String genero,
        String telefono,
        String fechaNacimiento,
        String passwordActual,
        String passwordNuevo
) {}
