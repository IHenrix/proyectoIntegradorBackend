package pe.edu.utp.pasajeya.app.service.impl;

import pe.edu.utp.pasajeya.app.dto.AdminDashboardDTO;
import pe.edu.utp.pasajeya.app.repository.AlertaRepository;
import pe.edu.utp.pasajeya.app.repository.PagoRepository;
import pe.edu.utp.pasajeya.app.repository.SuscripcionRepository;
import pe.edu.utp.pasajeya.app.repository.UsuarioRepository;
import pe.edu.utp.pasajeya.app.service.AdminDashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UsuarioRepository usuarioRepo;
    private final AlertaRepository alertaRepo;
    private final SuscripcionRepository suscripcionRepo;
    private final PagoRepository pagoRepo;

    public AdminDashboardServiceImpl(UsuarioRepository usuarioRepo,
                                      AlertaRepository alertaRepo,
                                      SuscripcionRepository suscripcionRepo,
                                      PagoRepository pagoRepo) {
        this.usuarioRepo = usuarioRepo;
        this.alertaRepo = alertaRepo;
        this.suscripcionRepo = suscripcionRepo;
        this.pagoRepo = pagoRepo;
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

        return new AdminDashboardDTO(
                usuariosPorRol,
                usuariosActivos,
                usuariosTotal - usuariosActivos,
                ingresosTotales,
                alertaRepo.countByActivaTrue(),
                suscripcionRepo.countByEstado("activa"),
                suscripcionRepo.countByEstado("vencida"),
                suscripcionRepo.countByEstado("cancelada")
        );
    }
}
