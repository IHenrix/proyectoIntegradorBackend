package com.pasajeya.dto;

/**
 * DTO de respuesta — representa un vuelo en el comparador.
 * Usa Java record (Java 21): inmutable, sin boilerplate.
 */
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
        String  semaforo          // "verde" | "amarillo" | "rojo"
) {}
