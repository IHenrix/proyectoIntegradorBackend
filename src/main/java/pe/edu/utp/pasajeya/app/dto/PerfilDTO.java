package pe.edu.utp.pasajeya.app.dto;

public record PerfilDTO(
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String genero,
        String telefono,
        String fechaNacimiento,
        String email,
        String rol
) {}
