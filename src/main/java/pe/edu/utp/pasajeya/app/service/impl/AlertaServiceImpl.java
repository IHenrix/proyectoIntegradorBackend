package pe.edu.utp.pasajeya.app.service.impl;

import pe.edu.utp.pasajeya.app.dto.AlertaDTO;
import pe.edu.utp.pasajeya.app.dto.CrearAlertaRequestDTO;
import pe.edu.utp.pasajeya.app.model.Alerta;
import pe.edu.utp.pasajeya.app.model.Tarifa;
import pe.edu.utp.pasajeya.app.model.Usuario;
import pe.edu.utp.pasajeya.app.model.Vuelo;
import pe.edu.utp.pasajeya.app.repository.AlertaRepository;
import pe.edu.utp.pasajeya.app.repository.HistorialPrecioRepository;
import pe.edu.utp.pasajeya.app.repository.TarifaRepository;
import pe.edu.utp.pasajeya.app.repository.UsuarioRepository;
import pe.edu.utp.pasajeya.app.repository.VueloRepository;
import pe.edu.utp.pasajeya.app.service.AlertaService;
import pe.edu.utp.pasajeya.app.service.WhatsAppNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class AlertaServiceImpl implements AlertaService {

    private final AlertaRepository alertaRepo;
    private final UsuarioRepository usuarioRepo;
    private final VueloRepository vueloRepo;
    private final TarifaRepository tarifaRepo;
    private final HistorialPrecioRepository historialRepo;
    private final WhatsAppNotificationService whatsAppService;

    public AlertaServiceImpl(AlertaRepository alertaRepo,
                             UsuarioRepository usuarioRepo,
                             VueloRepository vueloRepo,
                             TarifaRepository tarifaRepo,
                             HistorialPrecioRepository historialRepo,
                             WhatsAppNotificationService whatsAppService) {
        this.alertaRepo = alertaRepo;
        this.usuarioRepo = usuarioRepo;
        this.vueloRepo = vueloRepo;
        this.tarifaRepo = tarifaRepo;
        this.historialRepo = historialRepo;
        this.whatsAppService = whatsAppService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertaDTO> listar(String email) {
        return alertaRepo.findByUsuarioEmailOrderByFechaCreacionDesc(email).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public AlertaDTO crear(String email, CrearAlertaRequestDTO request) {
        Usuario usuario = usuarioRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Tarifa tarifa = resolverTarifa(request);

        Alerta alerta = new Alerta();
        alerta.setUsuario(usuario);
        alerta.setVuelo(tarifa.getVuelo());
        alerta.setTipoTarifa(tarifa.getTipo());
        alerta.setPrecioObjetivo(BigDecimal.valueOf(request.precioObjetivo()));
        alerta.setTelefono(request.telefono());
        alerta.setActiva(true);
        alerta.setFechaCreacion(LocalDateTime.now());

        return toDto(alertaRepo.save(alerta));
    }

    @Override
    @Transactional
    public AlertaDTO pausar(String email, Integer id) {
        Alerta alerta = alertaRepo.findByIdAndUsuarioEmail(id, email)
                .orElseThrow(() -> new RuntimeException("Alerta no encontrada"));
        alerta.setActiva(false);
        return toDto(alertaRepo.save(alerta));
    }

    @Override
    @Transactional
    public void eliminar(String email, Integer id) {
        Alerta alerta = alertaRepo.findByIdAndUsuarioEmail(id, email)
                .orElseThrow(() -> new RuntimeException("Alerta no encontrada"));
        alertaRepo.delete(alerta);
    }

    @Override
    @Transactional
    public void evaluarAlertasActivas() {
        LocalDateTime desde = LocalDateTime.now().minusDays(30);
        for (Alerta alerta : alertaRepo.findByActivaTrue()) {
            tarifaRepo.findFirstByVueloAndTipoOrderByPrecioAsc(alerta.getVuelo(), alerta.getTipoTarifa())
                    .ifPresent(tarifa -> evaluar(alerta, tarifa, desde));
        }
    }

    private void evaluar(Alerta alerta, Tarifa tarifa, LocalDateTime desde) {
        BigDecimal precioActual = tarifa.getPrecio();
        double promedio = historialRepo.calcularPromedio(
                alerta.getVuelo().getId(), alerta.getTipoTarifa(), desde
        ).orElse(precioActual.doubleValue());

        boolean bajoObjetivo = precioActual.compareTo(alerta.getPrecioObjetivo()) <= 0;
        boolean bajaDiez = precioActual.doubleValue() <= promedio * 0.90;
        boolean sinSpam = alerta.getUltimoPrecioNotificado() == null
                || precioActual.doubleValue() <= alerta.getUltimoPrecioNotificado().doubleValue() * 0.90;

        if ((bajoObjetivo || bajaDiez) && sinSpam) {
            double variacion = promedio == 0 ? 0 : ((precioActual.doubleValue() - promedio) / promedio) * 100.0;
            whatsAppService.enviarAlertaPrecio(alerta, precioActual, variacion);
            alerta.setUltimoPrecioNotificado(precioActual);
            alerta.setFechaUltimaNotificacion(LocalDateTime.now());
            alertaRepo.save(alerta);
        }
    }

    private Tarifa resolverTarifa(CrearAlertaRequestDTO request) {
        if (request.tarifaId() != null) {
            return tarifaRepo.findById(request.tarifaId().intValue())
                    .orElseThrow(() -> new RuntimeException("Tarifa no encontrada"));
        }

        String tipo = request.tipoTarifa() == null || request.tipoTarifa().isBlank()
                ? "basica"
                : request.tipoTarifa().trim().toLowerCase();
        if (request.origen() == null || request.destino() == null || request.fecha() == null) {
            throw new RuntimeException("Debes enviar tarifaId o ruta completa con fecha");
        }

        List<Tarifa> tarifas = vueloRepo.findByOrigenAndDestinoAndFechaSalida(
                        request.origen().trim(),
                        request.destino().trim(),
                        LocalDate.parse(request.fecha()))
                .stream()
                .map(v -> tarifaRepo.findFirstByVueloAndTipoOrderByPrecioAsc(v, tipo))
                .flatMap(java.util.Optional::stream)
                .sorted(Comparator.comparing(Tarifa::getPrecio))
                .toList();

        if (tarifas.isEmpty()) {
            throw new RuntimeException("No hay vuelos disponibles para crear la alerta");
        }
        return tarifas.get(0);
    }

    private AlertaDTO toDto(Alerta alerta) {
        Tarifa tarifa = tarifaRepo.findFirstByVueloAndTipoOrderByPrecioAsc(alerta.getVuelo(), alerta.getTipoTarifa())
                .orElse(null);
        Vuelo vuelo = alerta.getVuelo();
        Double precioActual = tarifa != null ? tarifa.getPrecio().doubleValue() : null;
        Long tarifaId = tarifa != null ? tarifa.getId().longValue() : null;
        String estado = Boolean.TRUE.equals(alerta.getActiva()) ? "Activa" : "Pausada";
        if (precioActual != null && precioActual <= alerta.getPrecioObjetivo().doubleValue()) {
            estado = "Precio objetivo alcanzado";
        }

        return new AlertaDTO(
                alerta.getId(),
                tarifaId,
                vuelo.getId(),
                nombreCorto(vuelo.getAerolinea().getNombre()),
                vuelo.getOrigen().trim(),
                vuelo.getDestino().trim(),
                vuelo.getFechaSalida().toString(),
                String.format("%02d:%02d", vuelo.getHoraSalida().getHour(), vuelo.getHoraSalida().getMinute()),
                alerta.getTipoTarifa(),
                alerta.getPrecioObjetivo().doubleValue(),
                precioActual,
                alerta.getTelefono(),
                alerta.getActiva(),
                alerta.getFechaCreacion() != null ? alerta.getFechaCreacion().toString() : null,
                estado
        );
    }

    private String nombreCorto(String nombre) {
        String upper = nombre.toUpperCase();
        if (upper.contains("LATAM")) return "LATAM";
        if (upper.contains("SKY")) return "Sky";
        if (upper.contains("JETSMART")) return "JetSmart";
        return nombre;
    }
}
