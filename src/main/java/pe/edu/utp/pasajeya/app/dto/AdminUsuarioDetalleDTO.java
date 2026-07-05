package pe.edu.utp.pasajeya.app.dto;

public record AdminUsuarioDetalleDTO(
        Integer id,
        String email,
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String genero,
        String telefono,
        String fechaNacimiento,
        String tipoDocumento,
        String nroDocumento,
        String rol,
        Boolean activo,
        Boolean emailVerificado,
        String fechaRegistro
) {}
