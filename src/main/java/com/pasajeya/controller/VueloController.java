package com.pasajeya.controller;

import com.pasajeya.config.DatabaseConnection;
import com.pasajeya.dto.VueloDTO;
import com.pasajeya.service.VueloService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vuelos")
public class VueloController {

    private static final Logger log = LoggerFactory.getLogger(VueloController.class);

    private final VueloService vueloService;

    public VueloController(VueloService vueloService) {
        this.vueloService = vueloService;
    }

    @GetMapping
    public ResponseEntity<List<VueloDTO>> buscar(
            @RequestParam String origen,
            @RequestParam String destino,
            @RequestParam String fecha,
            @RequestParam(defaultValue = "1") int pasajeros) {

        DatabaseConnection db = DatabaseConnection.getInstance();
        log.info("Request recibido → {} a {} | fecha: {} | BD: {}", origen, destino, fecha, db.getNombreBD());

        List<VueloDTO> vuelos = vueloService.buscarVuelos(origen, destino, fecha, pasajeros);

        log.info("Respondiendo {} vuelos encontrados", vuelos.size());

        return ResponseEntity.ok(vuelos);
    }
}
