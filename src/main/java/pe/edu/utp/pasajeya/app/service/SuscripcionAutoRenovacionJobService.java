package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.model.Pago;
import pe.edu.utp.pasajeya.app.model.Suscripcion;
import pe.edu.utp.pasajeya.app.repository.PagoRepository;
import pe.edu.utp.pasajeya.app.repository.SuscripcionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Auto-renueva (sin intervención del usuario) las suscripciones pagadas con
 * tarjeta que ya vencieron o vencen hoy — igual que Netflix/Spotify. Yape/Plin
 * nunca entran aquí (auto_renovar siempre queda en false para esos métodos),
 * porque no existe débito automático real para esos medios sin una tarjeta
 * detrás; el usuario debe renovarlos manualmente desde su perfil.
 */
@Service
public class SuscripcionAutoRenovacionJobService {

    private static final Logger log = LoggerFactory.getLogger(SuscripcionAutoRenovacionJobService.class);

    private final SuscripcionRepository suscripcionRepo;
    private final PagoRepository pagoRepo;

    public SuscripcionAutoRenovacionJobService(SuscripcionRepository suscripcionRepo,
                                                PagoRepository pagoRepo) {
        this.suscripcionRepo = suscripcionRepo;
        this.pagoRepo = pagoRepo;
    }

    @Scheduled(
            fixedRateString = "${app.autorenovacion.rate-ms:3600000}",
            initialDelayString = "${app.autorenovacion.initial-delay-ms:60000}"
    )
    @Transactional
    public void renovarAutomaticas() {
        LocalDate hoy = LocalDate.now();
        int total = 0;
        for (Suscripcion s : suscripcionRepo.findParaAutoRenovar(hoy)) {
            renovarUna(s);
            total++;
        }
        log.info("Job auto-renovación completado: {} suscripciones renovadas", total);
    }

    private void renovarUna(Suscripcion s) {
        Pago pagoOrigen = pagoRepo.findById(s.getIdPagoOrigen()).orElse(null);

        Pago nuevoPago = new Pago();
        nuevoPago.setPersona(s.getPersona());
        nuevoPago.setPlan(s.getPlan());
        nuevoPago.setMonto(s.getPlan().getPrecioMensual());
        nuevoPago.setMoneda("PEN");
        nuevoPago.setMetodo(s.getMetodoPago());
        nuevoPago.setEstado("aprobado");
        nuevoPago.setPasarela("manual");
        nuevoPago.setTokenPasarela(generarRefInterna("auto"));
        nuevoPago.setUltimosCuatro(pagoOrigen != null ? pagoOrigen.getUltimosCuatro() : null);
        nuevoPago.setMarcaTarjeta(pagoOrigen != null ? pagoOrigen.getMarcaTarjeta() : null);
        nuevoPago.setTitularTarjeta(pagoOrigen != null ? pagoOrigen.getTitularTarjeta() : null);
        nuevoPago.setEmailRecibo(pagoOrigen != null ? pagoOrigen.getEmailRecibo() : null);
        nuevoPago.setRefInterna(generarRefInterna(null));
        nuevoPago.setFechaPago(LocalDateTime.now());
        nuevoPago = pagoRepo.save(nuevoPago);

        // Se extiende desde la fecha_fin vieja (no desde "hoy"): si el job
        // corre con retraso, el usuario no pierde ni gana días de su plan.
        boolean anual = s.getPlan().getDuracionDias() >= 365;
        s.setFechaFin(anual ? s.getFechaFin().plusYears(1) : s.getFechaFin().plusMonths(1));
        s.setIdPagoOrigen(nuevoPago.getId());
        s.setPrecioPagado(nuevoPago.getMonto());
        suscripcionRepo.save(s);
    }

    private String generarRefInterna(String prefijo) {
        int numero = ThreadLocalRandom.current().nextInt(100000, 999999);
        return prefijo != null ? prefijo + "_" + numero : String.valueOf(numero);
    }
}
