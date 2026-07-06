package pe.edu.utp.pasajeya.app.service.impl;

import pe.edu.utp.pasajeya.app.dto.AdminDashboardDTO;
import pe.edu.utp.pasajeya.app.dto.AdminPrecioRutaSemanaDTO;
import pe.edu.utp.pasajeya.app.repository.AlertaRepository;
import pe.edu.utp.pasajeya.app.repository.HistorialPrecioRepository;
import pe.edu.utp.pasajeya.app.repository.PagoRepository;
import pe.edu.utp.pasajeya.app.repository.SuscripcionRepository;
import pe.edu.utp.pasajeya.app.repository.UsuarioRepository;
import pe.edu.utp.pasajeya.app.service.AdminDashboardService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    /** Cuántas rutas (las de mayor volumen de historial) se grafican en la serie de precios semanal. */
    private static final int TOP_RUTAS = 4;

    private final UsuarioRepository usuarioRepo;
    private final AlertaRepository alertaRepo;
    private final SuscripcionRepository suscripcionRepo;
    private final PagoRepository pagoRepo;
    private final HistorialPrecioRepository historialRepo;

    public AdminDashboardServiceImpl(UsuarioRepository usuarioRepo,
                                      AlertaRepository alertaRepo,
                                      SuscripcionRepository suscripcionRepo,
                                      PagoRepository pagoRepo,
                                      HistorialPrecioRepository historialRepo) {
        this.usuarioRepo = usuarioRepo;
        this.alertaRepo = alertaRepo;
        this.suscripcionRepo = suscripcionRepo;
        this.pagoRepo = pagoRepo;
        this.historialRepo = historialRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardDTO obtenerMetricas() {
        Map<String, Long> usuariosPorRol = new LinkedHashMap<>();
        for (Object[] fila : usuarioRepo.contarPorRol()) {
            usuariosPorRol.put((String) fila[0], (Long) fila[1]);
        }

        long usuariosActivos = usuarioRepo.countByActivoTrue();
        long usuariosTotal = usuarioRepo.count();

        BigDecimal ingresosTotales = pagoRepo.sumarIngresosTotales().orElse(BigDecimal.ZERO);

        Map<String, BigDecimal> ingresosPorMes = new LinkedHashMap<>();
        LocalDateTime desde12Meses = LocalDateTime.now().minusMonths(11).withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        for (Object[] fila : pagoRepo.sumarIngresosPorMes(desde12Meses)) {
            ingresosPorMes.put((String) fila[0], (BigDecimal) fila[1]);
        }

        Map<String, Long> alertasPorAerolinea = new LinkedHashMap<>();
        for (Object[] fila : alertaRepo.contarActivasPorAerolinea()) {
            alertasPorAerolinea.put((String) fila[0], (Long) fila[1]);
        }

        return new AdminDashboardDTO(
                usuariosPorRol,
                usuariosActivos,
                usuariosTotal - usuariosActivos,
                ingresosTotales,
                alertaRepo.countByActivaTrue(),
                suscripcionRepo.countByEstado("activa"),
                suscripcionRepo.countByEstado("vencida"),
                suscripcionRepo.countByEstado("cancelada"),
                ingresosPorMes,
                alertasPorAerolinea
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminPrecioRutaSemanaDTO> obtenerPreciosPorRuta() {
        LocalDateTime desde90Dias = LocalDateTime.now().minusDays(90);

        // Las TOP_RUTAS con más registros de historial, para no graficar
        // decenas de rutas irrelevantes con pocos puntos de dato.
        Set<String> rutasTop = new LinkedHashSet<>();
        for (Object[] fila : historialRepo.contarPorRuta(PageRequest.of(0, TOP_RUTAS))) {
            rutasTop.add(fila[0] + "-" + fila[1]);
        }

        List<AdminPrecioRutaSemanaDTO> resultado = new java.util.ArrayList<>();
        for (Object[] fila : historialRepo.promedioSemanalPorRuta(desde90Dias)) {
            String origen = (String) fila[0];
            String destino = (String) fila[1];
            String ruta = origen + "-" + destino;
            if (!rutasTop.contains(ruta)) continue;

            String semana = (String) fila[2];
            double promedio = (double) fila[3];
            resultado.add(new AdminPrecioRutaSemanaDTO(ruta, semana, BigDecimal.valueOf(promedio)));
        }
        return resultado;
    }
}
