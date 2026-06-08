package pe.edu.utp.pasajeya.app.dto;

public record VueloDTO(
        Long    id,
        String  aerolinea,
        String  origen,
        String  destino,
        String  fecha,
        String  horaSalida,
        String  horaLlegada,
        String  duracion,
        Double  precio,
        String  tipoTarifa,
        Boolean incluyeEquipaje,
        Integer equipajeBodegaKg,
        Integer equipajeManoKg,
        Boolean permiteReembolso,
        Boolean asientoSeleccionable,
        String  semaforo,
        String  urlAerolinea
) {}
