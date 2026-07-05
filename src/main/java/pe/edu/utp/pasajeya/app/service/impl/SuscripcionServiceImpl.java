package pe.edu.utp.pasajeya.app.service.impl;

import pe.edu.utp.pasajeya.app.dto.PagoRequestDTO;
import pe.edu.utp.pasajeya.app.dto.SuscripcionDTO;
import pe.edu.utp.pasajeya.app.model.Pago;
import pe.edu.utp.pasajeya.app.model.Persona;
import pe.edu.utp.pasajeya.app.model.Plan;
import pe.edu.utp.pasajeya.app.model.Rol;
import pe.edu.utp.pasajeya.app.model.Suscripcion;
import pe.edu.utp.pasajeya.app.model.Usuario;
import pe.edu.utp.pasajeya.app.repository.PagoRepository;
import pe.edu.utp.pasajeya.app.repository.PlanRepository;
import pe.edu.utp.pasajeya.app.repository.RolRepository;
import pe.edu.utp.pasajeya.app.repository.SuscripcionRepository;
import pe.edu.utp.pasajeya.app.repository.UsuarioRepository;
import pe.edu.utp.pasajeya.app.service.SuscripcionService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Procesa pagos de suscripción SIN conectarse a una pasarela real (Culqi,
 * Stripe, etc.): valida los datos igual que lo haría una pasarela (Luhn,
 * expiración) y persiste todo en BD como si el cobro hubiera ocurrido.
 * Es la simulación acordada para el proyecto académico — el monto y la
 * duración del plan siempre se calculan desde Plan en BD, nunca desde lo
 * que envía el cliente, para que no sea posible "pagar" un monto arbitrario.
 */
@Service
public class SuscripcionServiceImpl implements SuscripcionService {

    private final UsuarioRepository     usuarioRepo;
    private final PlanRepository        planRepo;
    private final RolRepository         rolRepo;
    private final PagoRepository        pagoRepo;
    private final SuscripcionRepository suscripcionRepo;

    public SuscripcionServiceImpl(UsuarioRepository usuarioRepo,
                                   PlanRepository planRepo,
                                   RolRepository rolRepo,
                                   PagoRepository pagoRepo,
                                   SuscripcionRepository suscripcionRepo) {
        this.usuarioRepo     = usuarioRepo;
        this.planRepo        = planRepo;
        this.rolRepo         = rolRepo;
        this.pagoRepo        = pagoRepo;
        this.suscripcionRepo = suscripcionRepo;
    }

    private static final String NOMBRE_PLAN_MENSUAL = "Premium Mensual";
    private static final String NOMBRE_PLAN_ANUAL   = "Premium Anual";
    private static final int    DIAS_VENTANA_RENOVACION = 7;

    @Override
    @Transactional
    public SuscripcionDTO pagar(String email, PagoRequestDTO request) {
        Usuario usuario = usuarioRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Persona persona = usuario.getPersona();
        LocalDate hoy = LocalDate.now();

        // Ya tiene un plan activo vigente: en general no se permite "pagar de
        // nuevo" por encima (evita doble cobro accidental por doble click,
        // etc.), salvo que falten pocos días para vencer — ahí sí se permite
        // adelantar la renovación (típico de Yape/Plin, que no tienen cobro
        // automático) sumando un período más desde la fecha_fin actual, en
        // vez de cortar el servicio y obligar a esperar a que venza.
        Optional<Suscripcion> vigenteActual = suscripcionRepo.findVigente(persona.getId(), hoy);
        if (vigenteActual.isPresent()) {
            Suscripcion vigente = vigenteActual.get();
            boolean puedeAdelantarRenovacion = !vigente.getFechaFin().isAfter(hoy.plusDays(DIAS_VENTANA_RENOVACION));
            if (!puedeAdelantarRenovacion) {
                throw new RuntimeException("Ya tienes una suscripción activa. Cancélala antes de contratar otra.");
            }
            return adelantarRenovacion(usuario, vigente, request);
        }

        // Canceló pero su período pagado aún no vence: reactivar en vez de
        // cobrar de nuevo por días que ya tenía cubiertos (como reactivar
        // Netflix/Spotify antes de que corte el servicio).
        Optional<Suscripcion> canceladaVigente = suscripcionRepo.findCanceladaVigente(persona.getId(), hoy);
        if (canceladaVigente.isPresent()) {
            Suscripcion reactivada = canceladaVigente.get();
            reactivada.setEstado("activa");
            reactivada.setAutoRenovar(esTarjeta(request.metodo()));
            reactivada = suscripcionRepo.save(reactivada);
            subirRolPremium(usuario);

            String ref = pagoRepo.findById(reactivada.getIdPagoOrigen())
                    .map(Pago::getRefInterna)
                    .orElse("—");
            return toDto(reactivada, ref);
        }

        String nombrePlan = "anual".equals(request.plan()) ? NOMBRE_PLAN_ANUAL : NOMBRE_PLAN_MENSUAL;
        Plan plan = planRepo.findByNombre(nombrePlan)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado: " + nombrePlan));

        validarMetodoPago(request);

        String ultimosCuatro = esTarjeta(request.metodo())
                ? StringUtils.right(soloDigitos(request.numeroTarjeta()), 4)
                : null;

        LocalDate fin = "anual".equals(request.plan()) ? hoy.plusYears(1) : hoy.plusMonths(1);

        Pago pago = new Pago();
        pago.setPersona(persona);
        pago.setPlan(plan);
        pago.setMonto(plan.getPrecioMensual());
        pago.setMoneda("PEN");
        pago.setMetodo(request.metodo());
        pago.setEstado("aprobado");
        pago.setPasarela("manual");
        pago.setTokenPasarela(generarRefInterna("sim"));
        pago.setUltimosCuatro(ultimosCuatro);
        pago.setMarcaTarjeta(esTarjeta(request.metodo()) ? detectarMarca(request.numeroTarjeta()) : null);
        pago.setTitularTarjeta(esTarjeta(request.metodo()) ? StringUtils.trimToNull(request.titular()) : null);
        pago.setEmailRecibo(StringUtils.defaultIfBlank(request.emailRecibo(), email));
        pago.setRefInterna(generarRefInterna(null));
        pago.setFechaPago(LocalDateTime.now());
        pago = pagoRepo.save(pago);

        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setPersona(persona);
        suscripcion.setPlan(plan);
        suscripcion.setIdPagoOrigen(pago.getId());
        suscripcion.setPrecioPagado(plan.getPrecioMensual());
        suscripcion.setMaxAlertasSnapshot(plan.getMaxAlertas());
        suscripcion.setFechaInicio(hoy);
        suscripcion.setFechaFin(fin);
        suscripcion.setEstado("activa");
        suscripcion.setMetodoPago(request.metodo());
        suscripcion.setAutoRenovar(esTarjeta(request.metodo()));
        suscripcion = suscripcionRepo.save(suscripcion);

        subirRolPremium(usuario);

        return toDto(suscripcion, pago.getRefInterna());
    }

    /**
     * El usuario ya tiene un plan activo, pero está a pocos días de vencer y
     * pide renovar por adelantado (típico de Yape/Plin, que no tienen cobro
     * automático). En vez de cortar el servicio y esperar a que venza, se
     * suma un período más desde la fecha_fin actual — no se crea una fila de
     * suscripcion aparte, se extiende la misma.
     */
    private SuscripcionDTO adelantarRenovacion(Usuario usuario, Suscripcion vigente, PagoRequestDTO request) {
        validarMetodoPago(request);

        String ultimosCuatro = esTarjeta(request.metodo())
                ? StringUtils.right(soloDigitos(request.numeroTarjeta()), 4)
                : null;

        Pago pago = new Pago();
        pago.setPersona(vigente.getPersona());
        pago.setPlan(vigente.getPlan());
        pago.setMonto(vigente.getPlan().getPrecioMensual());
        pago.setMoneda("PEN");
        pago.setMetodo(request.metodo());
        pago.setEstado("aprobado");
        pago.setPasarela("manual");
        pago.setTokenPasarela(generarRefInterna("sim"));
        pago.setUltimosCuatro(ultimosCuatro);
        pago.setMarcaTarjeta(esTarjeta(request.metodo()) ? detectarMarca(request.numeroTarjeta()) : null);
        pago.setTitularTarjeta(esTarjeta(request.metodo()) ? StringUtils.trimToNull(request.titular()) : null);
        pago.setEmailRecibo(StringUtils.defaultIfBlank(request.emailRecibo(), usuario.getEmail()));
        pago.setRefInterna(generarRefInterna(null));
        pago.setFechaPago(LocalDateTime.now());
        pago = pagoRepo.save(pago);

        boolean anual = vigente.getPlan().getDuracionDias() >= 365;
        vigente.setFechaFin(anual ? vigente.getFechaFin().plusYears(1) : vigente.getFechaFin().plusMonths(1));
        vigente.setIdPagoOrigen(pago.getId());
        vigente.setPrecioPagado(pago.getMonto());
        vigente.setMetodoPago(request.metodo());
        vigente.setAutoRenovar(esTarjeta(request.metodo()));
        vigente = suscripcionRepo.save(vigente);

        return toDto(vigente, pago.getRefInterna());
    }

    private void subirRolPremium(Usuario usuario) {
        Rol rolPremium = rolRepo.findByNombre("usuario_premium")
                .orElseThrow(() -> new RuntimeException("Rol usuario_premium no encontrado en BD"));
        usuario.setRol(rolPremium);
        usuarioRepo.save(usuario);
    }

    @Override
    @Transactional
    public void cancelar(String email) {
        Usuario usuario = usuarioRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Suscripcion vigente = suscripcionRepo
                .findVigente(usuario.getPersona().getId(), LocalDate.now())
                .orElseThrow(() -> new RuntimeException("No tienes una suscripción activa para cancelar"));

        // El usuario conserva premium hasta fechaFin; el rol se degrada más
        // adelante mediante expirarYDegradar() (lazy expiry), no al instante.
        vigente.setEstado("cancelada");
        suscripcionRepo.save(vigente);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private void validarMetodoPago(PagoRequestDTO request) {
        if (!esTarjeta(request.metodo())) return;

        String numero = soloDigitos(request.numeroTarjeta());
        if (StringUtils.isBlank(numero) || !luhnValido(numero)) {
            throw new RuntimeException("El número de tarjeta no es válido");
        }
        if (StringUtils.isBlank(request.expira()) || !request.expira().matches("^\\d{2}/\\d{2}$")) {
            throw new RuntimeException("La fecha de vencimiento no es válida");
        }
        if (StringUtils.isBlank(request.titular())) {
            throw new RuntimeException("El titular de la tarjeta es requerido");
        }
    }

    private boolean esTarjeta(String metodo) {
        return "tarjeta_credito".equals(metodo) || "tarjeta_debito".equals(metodo);
    }

    private String soloDigitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    private boolean luhnValido(String numero) {
        int suma = 0;
        boolean alternar = false;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(numero.charAt(i));
            if (alternar) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            suma += n;
            alternar = !alternar;
        }
        return suma % 10 == 0;
    }

    private String detectarMarca(String numero) {
        String n = soloDigitos(numero);
        if (n.startsWith("4")) return "visa";
        if (n.matches("^5[1-5].*") || n.matches("^2[2-7].*")) return "mastercard";
        if (n.matches("^3[47].*")) return "amex";
        return "desconocida";
    }

    private String generarRefInterna(String prefijo) {
        int numero = ThreadLocalRandom.current().nextInt(100000, 999999);
        return prefijo != null ? prefijo + "_" + numero : String.valueOf(numero);
    }

    private SuscripcionDTO toDto(Suscripcion s, String refInterna) {
        String tipo = s.getPlan().getDuracionDias() >= 365 ? "anual" : "mensual";
        return new SuscripcionDTO(
                s.getId(),
                s.getPlan().getNombre(),
                tipo,
                s.getPrecioPagado(),
                s.getFechaInicio().toString(),
                s.getFechaFin().toString(),
                s.getEstado(),
                s.getMetodoPago(),
                refInterna,
                Boolean.TRUE.equals(s.getAutoRenovar())
        );
    }
}
