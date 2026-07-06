package pe.edu.utp.pasajeya.app.service.impl;

import pe.edu.utp.pasajeya.app.dto.AdminJobEstadoDTO;
import pe.edu.utp.pasajeya.app.repository.HistorialPrecioRepository;
import pe.edu.utp.pasajeya.app.repository.TarifaRepository;
import pe.edu.utp.pasajeya.app.repository.VueloRepository;
import pe.edu.utp.pasajeya.app.service.AdminVueloJobService;
import pe.edu.utp.pasajeya.app.service.HistorialPrecioJobService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AdminVueloJobServiceImpl implements AdminVueloJobService {

    private final VueloRepository vueloRepo;
    private final TarifaRepository tarifaRepo;
    private final HistorialPrecioRepository historialRepo;
    private final HistorialPrecioJobService jobService;

    public AdminVueloJobServiceImpl(VueloRepository vueloRepo,
                                     TarifaRepository tarifaRepo,
                                     HistorialPrecioRepository historialRepo,
                                     HistorialPrecioJobService jobService) {
        this.vueloRepo = vueloRepo;
        this.tarifaRepo = tarifaRepo;
        this.historialRepo = historialRepo;
        this.jobService = jobService;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminJobEstadoDTO obtenerEstado() {
        LocalDateTime ultima = jobService.getUltimaEjecucion();
        long tasaMs = jobService.getTasaCapturaMs();

        return new AdminJobEstadoDTO(
                ultima == null ? null : ultima.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                ultima == null ? null : ultima.plusNanos(tasaMs * 1_000_000).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                tarifaRepo.count(),
                vueloRepo.count(),
                historialRepo.count(),
                tasaMs
        );
    }

    @Override
    public void ejecutarAhora() {
        jobService.ejecutarAhora();
    }
}
