package pe.edu.utp.pasajeya.app.service.impl;

import pe.edu.utp.pasajeya.app.dto.AdminPagoDTO;
import pe.edu.utp.pasajeya.app.dto.AdminSuscripcionDTO;
import pe.edu.utp.pasajeya.app.model.Pago;
import pe.edu.utp.pasajeya.app.model.Persona;
import pe.edu.utp.pasajeya.app.model.Suscripcion;
import pe.edu.utp.pasajeya.app.model.Usuario;
import pe.edu.utp.pasajeya.app.repository.PagoRepository;
import pe.edu.utp.pasajeya.app.repository.SuscripcionRepository;
import pe.edu.utp.pasajeya.app.repository.UsuarioRepository;
import pe.edu.utp.pasajeya.app.service.AdminSuscripcionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AdminSuscripcionServiceImpl implements AdminSuscripcionService {

    private final SuscripcionRepository suscripcionRepo;
    private final PagoRepository pagoRepo;
    private final UsuarioRepository usuarioRepo;

    public AdminSuscripcionServiceImpl(SuscripcionRepository suscripcionRepo,
                                        PagoRepository pagoRepo,
                                        UsuarioRepository usuarioRepo) {
        this.suscripcionRepo = suscripcionRepo;
        this.pagoRepo = pagoRepo;
        this.usuarioRepo = usuarioRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminSuscripcionDTO> listarSuscripciones() {
        return suscripcionRepo.findAllByOrderByFechaInicioDesc().stream()
                .map(this::toSuscripcionDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminPagoDTO> listarPagos() {
        return pagoRepo.findAllByOrderByFechaPagoDesc().stream()
                .map(this::toPagoDto)
                .toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private AdminSuscripcionDTO toSuscripcionDto(Suscripcion s) {
        Optional<Usuario> usuario = usuarioRepo.findByPersonaId(s.getPersona().getId());
        String tipo = s.getPlan().getDuracionDias() >= 365 ? "anual" : "mensual";

        return new AdminSuscripcionDTO(
                s.getId(),
                usuario.map(Usuario::getEmail).orElse("—"),
                usuario.map(this::nombreCompleto).orElse("—"),
                s.getPlan().getNombre(),
                tipo,
                s.getPrecioPagado(),
                s.getFechaInicio().toString(),
                s.getFechaFin().toString(),
                s.getEstado(),
                s.getMetodoPago(),
                Boolean.TRUE.equals(s.getAutoRenovar())
        );
    }

    private AdminPagoDTO toPagoDto(Pago p) {
        Optional<Usuario> usuario = usuarioRepo.findByPersonaId(p.getPersona().getId());

        return new AdminPagoDTO(
                p.getId(),
                usuario.map(Usuario::getEmail).orElse("—"),
                usuario.map(this::nombreCompleto).orElse("—"),
                p.getMonto(),
                p.getMoneda(),
                p.getMetodo(),
                p.getEstado(),
                p.getRefInterna(),
                p.getFechaPago().toString()
        );
    }

    private String nombreCompleto(Usuario u) {
        Persona p = u.getPersona();
        return p != null ? (p.getNombre() + " " + p.getApellidoPaterno()).trim() : "—";
    }
}
