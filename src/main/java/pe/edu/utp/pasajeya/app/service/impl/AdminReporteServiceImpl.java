package pe.edu.utp.pasajeya.app.service.impl;

import pe.edu.utp.pasajeya.app.dto.AdminReporteResumenDTO;
import pe.edu.utp.pasajeya.app.repository.HistorialPrecioRepository;
import pe.edu.utp.pasajeya.app.repository.PagoRepository;
import pe.edu.utp.pasajeya.app.repository.PersonaRepository;
import pe.edu.utp.pasajeya.app.repository.SuscripcionRepository;
import pe.edu.utp.pasajeya.app.repository.UsuarioRepository;
import pe.edu.utp.pasajeya.app.service.AdminReporteService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminReporteServiceImpl implements AdminReporteService {

    private final UsuarioRepository usuarioRepo;
    private final PersonaRepository personaRepo;
    private final PagoRepository pagoRepo;
    private final SuscripcionRepository suscripcionRepo;
    private final HistorialPrecioRepository historialRepo;

    public AdminReporteServiceImpl(UsuarioRepository usuarioRepo,
                                    PersonaRepository personaRepo,
                                    PagoRepository pagoRepo,
                                    SuscripcionRepository suscripcionRepo,
                                    HistorialPrecioRepository historialRepo) {
        this.usuarioRepo = usuarioRepo;
        this.personaRepo = personaRepo;
        this.pagoRepo = pagoRepo;
        this.suscripcionRepo = suscripcionRepo;
        this.historialRepo = historialRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminReporteResumenDTO obtenerResumen() {
        long usuariosTotal = usuarioRepo.count();
        long suscripcionesActivas = suscripcionRepo.countByEstado("activa");
        BigDecimal ingresosTotales = pagoRepo.sumarIngresosTotales().orElse(BigDecimal.ZERO);

        BigDecimal tasaConversion = usuariosTotal > 0
                ? BigDecimal.valueOf(suscripcionesActivas)
                        .divide(BigDecimal.valueOf(usuariosTotal), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal ingresoPromedio = suscripcionesActivas > 0
                ? ingresosTotales.divide(BigDecimal.valueOf(suscripcionesActivas), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String rutaMasConsultada = historialRepo.contarPorRuta(PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(fila -> fila[0] + "-" + fila[1])
                .orElse("—");

        LocalDate hoy = LocalDate.now();
        LocalDate inicioMesActual = hoy.withDayOfMonth(1);
        LocalDate inicioMesAnterior = inicioMesActual.minusMonths(1);
        LocalDateTime inicioMesActualDT = inicioMesActual.atStartOfDay();
        LocalDateTime inicioMesAnteriorDT = inicioMesAnterior.atStartOfDay();
        LocalDateTime finMesActualDT = hoy.plusDays(1).atStartOfDay();

        long usuariosNuevosMesActual = personaRepo.countByFechaRegistroBetween(inicioMesActualDT, finMesActualDT);
        long usuariosNuevosMesAnterior = personaRepo.countByFechaRegistroBetween(inicioMesAnteriorDT, inicioMesActualDT);

        BigDecimal ingresosMesActual = pagoRepo.sumarIngresosEntre(inicioMesActualDT, finMesActualDT).orElse(BigDecimal.ZERO);
        BigDecimal ingresosMesAnterior = pagoRepo.sumarIngresosEntre(inicioMesAnteriorDT, inicioMesActualDT).orElse(BigDecimal.ZERO);

        long suscripcionesNuevasMesActual = suscripcionRepo.countByFechaInicioBetween(inicioMesActual, hoy);
        long suscripcionesNuevasMesAnterior = suscripcionRepo.countByFechaInicioBetween(inicioMesAnterior, inicioMesActual.minusDays(1));

        return new AdminReporteResumenDTO(
                tasaConversion,
                ingresoPromedio,
                rutaMasConsultada,
                usuariosNuevosMesActual,
                usuariosNuevosMesAnterior,
                ingresosMesActual,
                ingresosMesAnterior,
                suscripcionesNuevasMesActual,
                suscripcionesNuevasMesAnterior
        );
    }
}
