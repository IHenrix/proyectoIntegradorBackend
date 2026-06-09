package pe.edu.utp.pasajeya.app.dto;

public record AlertaDTO(
        Integer id,
        Long tarifaId,
        Integer idVuelo,
        String aerolinea,
        String origen,
        String destino,
        String fecha,
        String horaSalida,
        String tipoTarifa,
        Double precioObjetivo,
        Double precioActual,
        String telefono,
        Boolean activa,
        String fechaCreacion,
        String estado
) {}
