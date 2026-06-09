package pe.edu.utp.pasajeya.app.controller;

import pe.edu.utp.pasajeya.app.dto.VueloDTO;
import pe.edu.utp.pasajeya.app.dto.VueloDetalleDTO;
import pe.edu.utp.pasajeya.app.service.VueloExcelService;
import pe.edu.utp.pasajeya.app.service.VueloDetalleService;
import pe.edu.utp.pasajeya.app.service.VueloService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/vuelos")
public class VueloController {

    private static final Logger log = LoggerFactory.getLogger(VueloController.class);

    private final VueloService      vueloService;
    private final VueloExcelService excelService;
    private final VueloDetalleService detalleService;

    public VueloController(VueloService vueloService,
                           VueloExcelService excelService,
                           VueloDetalleService detalleService) {
        this.vueloService = vueloService;
        this.excelService = excelService;
        this.detalleService = detalleService;
    }

    /** GET /api/vuelos?origen=LIM&destino=CUZ&fecha=2026-06-15&pasajeros=1 */
    @GetMapping
    public ResponseEntity<List<VueloDTO>> buscar(
            @RequestParam String origen,
            @RequestParam String destino,
            @RequestParam String fecha,
            @RequestParam(defaultValue = "1") @Min(1) @Max(4) int pasajeros) {

        log.info("Request recibido → {} a {} | fecha: {}", origen, destino, fecha);

        List<VueloDTO> vuelos = vueloService.buscarVuelos(origen, destino, fecha, pasajeros);
        log.info("Respondiendo {} vuelos encontrados", vuelos.size());

        return ResponseEntity.ok(vuelos);
    }

    /** GET /api/vuelos/tarifas/123 */
    @GetMapping("/tarifas/{tarifaId}")
    public ResponseEntity<VueloDetalleDTO> detalle(@PathVariable Long tarifaId) {
        return ResponseEntity.ok(detalleService.obtenerDetalle(tarifaId));
    }

    /** GET /api/vuelos/exportar?origen=LIM&destino=CUZ&fecha=2026-06-15 */
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(
            @RequestParam String origen,
            @RequestParam String destino,
            @RequestParam String fecha,
            @RequestParam(defaultValue = "1") @Min(1) @Max(4) int pasajeros) throws IOException {

        log.info("Exportando Excel → {} a {} ({})", origen, destino, fecha);

        List<VueloDTO> vuelos = vueloService.buscarVuelos(origen, destino, fecha, pasajeros);
        byte[] excel = excelService.generarExcel(vuelos);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=vuelos-pasajeya.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
