package com.pasajeya.controller;

import com.pasajeya.config.DatabaseConnection;
import com.pasajeya.dto.SistemaInfoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sistema")
public class SistemaController {

    private static final Logger log = LoggerFactory.getLogger(SistemaController.class);

    @GetMapping("/info")
    public ResponseEntity<SistemaInfoDTO> info() {

        log.info("Consulta de info del sistema recibida");

        DatabaseConnection db = DatabaseConnection.getInstance();

        log.info("Singleton hash: #{}", System.identityHashCode(db));

        SistemaInfoDTO dto = new SistemaInfoDTO(
            "Singleton (Double-Checked Locking)",
            "instancia #" + System.identityHashCode(db),
            db.getUrl(),
            db.getNombreBD(),
            db.getPuerto(),
            "configurado — BD pendiente de conexion"
        );

        return ResponseEntity.ok(dto);
    }
}
