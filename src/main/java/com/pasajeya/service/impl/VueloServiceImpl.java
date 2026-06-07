package com.pasajeya.service.impl;

import com.google.common.collect.ImmutableList;
import com.pasajeya.dto.VueloDTO;
import com.pasajeya.service.VueloService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VueloServiceImpl implements VueloService {

    private static final Logger log = LoggerFactory.getLogger(VueloServiceImpl.class);

    @Override
    public List<VueloDTO> buscarVuelos(String origen, String destino, String fecha, int pasajeros) {

        log.info("Buscando vuelos: {} → {} ({})", origen, destino, fecha);

        // Google Guava — ImmutableList para datos mock (lista no modificable)
        List<VueloDTO> vuelos = ImmutableList.of(
            new VueloDTO(1L, "LATAM",    origen, destino, fecha, "06:00", "07:30", "1h 30m", 189.00, "Básico",   false, "verde"),
            new VueloDTO(2L, "Sky",      origen, destino, fecha, "10:15", "11:45", "1h 30m", 145.00, "Básico",   false, "amarillo"),
            new VueloDTO(3L, "JetSmart", origen, destino, fecha, "15:30", "17:00", "1h 30m", 119.00, "Básico",   false, "rojo"),
            new VueloDTO(4L, "LATAM",    origen, destino, fecha, "18:45", "20:15", "1h 30m", 215.00, "Flexible", true,  "amarillo")
        );

        log.info("Se encontraron {} vuelos", vuelos.size());

        return vuelos;
    }
}
