package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.model.HistorialPrecio;
import pe.edu.utp.pasajeya.app.model.Tarifa;
import pe.edu.utp.pasajeya.app.repository.HistorialPrecioRepository;
import pe.edu.utp.pasajeya.app.repository.TarifaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class HistorialPrecioJobService {

    private static final Logger log = LoggerFactory.getLogger(HistorialPrecioJobService.class);

    private final TarifaRepository tarifaRepo;
    private final HistorialPrecioRepository historialRepo;
    private final AlertaService alertaService;

    public HistorialPrecioJobService(TarifaRepository tarifaRepo,
                                     HistorialPrecioRepository historialRepo,
                                     AlertaService alertaService) {
        this.tarifaRepo = tarifaRepo;
        this.historialRepo = historialRepo;
        this.alertaService = alertaService;
    }

    @Scheduled(
            fixedRateString = "${app.historial.capture-rate-ms:21600000}",
            initialDelayString = "${app.historial.initial-delay-ms:600000}"
    )
    @Transactional
    public void capturarPrecios() {
        LocalDateTime ahora = LocalDateTime.now();
        int total = 0;
        for (Tarifa tarifa : tarifaRepo.findAll()) {
            HistorialPrecio hp = new HistorialPrecio();
            hp.setVuelo(tarifa.getVuelo());
            hp.setTipoTarifa(tarifa.getTipo());
            hp.setPrecio(tarifa.getPrecio());
            hp.setFechaCaptura(ahora);
            historialRepo.save(hp);
            total++;
        }
        alertaService.evaluarAlertasActivas();
        log.info("Job historial completado: {} precios capturados", total);
    }
}
