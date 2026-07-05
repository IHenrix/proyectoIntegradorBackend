package pe.edu.utp.pasajeya.app.dto;

public record AdminUsuarioListadoDTO(
        Integer id,
        String email,
        String nombreCompleto,
        String rol,
        Boolean activo,
        Boolean emailVerificado,
        String fechaRegistro
) {}
