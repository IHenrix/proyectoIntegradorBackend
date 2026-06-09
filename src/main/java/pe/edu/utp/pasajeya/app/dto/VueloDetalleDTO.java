package pe.edu.utp.pasajeya.app.dto;

import java.util.List;

public record VueloDetalleDTO(
        Long idTarifa,
        Integer idVuelo,
        String aerolinea,
        String origen,
        String destino,
        String fecha,
        String horaSalida,
        String horaLlegada,
        String duracion,
        Double precioActual,
        String tipoTarifa,
        Boolean incluyeEquipaje,
        Integer equipajeBodegaKg,
        Integer equipajeManoKg,
        Boolean permiteReembolso,
        Boolean asientoSeleccionable,
        String semaforo,
        String urlAerolinea,
        List<PrecioPuntoDTO> historico,
        List<PrediccionPuntoDTO> prediccion,
        String recomendacion
) {}
