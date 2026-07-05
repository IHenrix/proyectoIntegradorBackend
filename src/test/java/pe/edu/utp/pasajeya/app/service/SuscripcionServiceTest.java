package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.PagoRequestDTO;
import pe.edu.utp.pasajeya.app.dto.SuscripcionDTO;
import pe.edu.utp.pasajeya.app.model.*;
import pe.edu.utp.pasajeya.app.repository.*;
import pe.edu.utp.pasajeya.app.service.impl.SuscripcionServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Verifica el flujo completo de pago simulado: persistencia en pago +
 * suscripcion, y que Usuario.rol suba a premium — sin lo cual el usuario
 * pagaría y seguiría bloqueado en los endpoints reales (AlertaController
 * decide todo por rol, no por la tabla suscripcion).
 */
@ExtendWith(MockitoExtension.class)
class SuscripcionServiceTest {

    @Mock private UsuarioRepository usuarioRepo;
    @Mock private PlanRepository planRepo;
    @Mock private RolRepository rolRepo;
    @Mock private PagoRepository pagoRepo;
    @Mock private SuscripcionRepository suscripcionRepo;

    @InjectMocks
    private SuscripcionServiceImpl suscripcionService;

    private Usuario usuarioFree;
    private Persona persona;
    private Plan planMensual;
    private Plan planAnual;
    private Rol rolFree;
    private Rol rolPremium;

    @BeforeEach
    void setUp() {
        rolFree = new Rol();
        rolFree.setId(1);
        rolFree.setNombre("usuario_free");

        rolPremium = new Rol();
        rolPremium.setId(2);
        rolPremium.setNombre("usuario_premium");

        persona = new Persona();
        persona.setId(10);
        persona.setNombre("Enrique");
        persona.setApellidoPaterno("Prada");

        usuarioFree = new Usuario();
        usuarioFree.setId(1);
        usuarioFree.setEmail("enrique.pdg@gmail.com");
        usuarioFree.setRol(rolFree);
        usuarioFree.setPersona(persona);

        planMensual = new Plan();
        planMensual.setId(2);
        planMensual.setNombre("Premium Mensual");
        planMensual.setPrecioMensual(new BigDecimal("15.00"));
        planMensual.setDuracionDias(30);
        planMensual.setMaxAlertas(999);

        planAnual = new Plan();
        planAnual.setId(3);
        planAnual.setNombre("Premium Anual");
        planAnual.setPrecioMensual(new BigDecimal("120.00"));
        planAnual.setDuracionDias(365);
        planAnual.setMaxAlertas(999);
    }

    // ═══════════════════════════════════════════════════
    // PAGO CON TARJETA — debe crear pago + suscripcion + subir rol
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("pagar() con tarjeta válida crea pago, suscripción y sube el rol a premium")
    void pagar_conTarjetaValida_creaTodoYSubeRol() {
        when(usuarioRepo.findByEmail("enrique.pdg@gmail.com")).thenReturn(Optional.of(usuarioFree));
        when(planRepo.findByNombre("Premium Mensual")).thenReturn(Optional.of(planMensual));
        when(rolRepo.findByNombre("usuario_premium")).thenReturn(Optional.of(rolPremium));

        when(pagoRepo.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            p.setId(100);
            return p;
        });
        when(suscripcionRepo.save(any(Suscripcion.class))).thenAnswer(inv -> {
            Suscripcion s = inv.getArgument(0);
            s.setId(200);
            return s;
        });

        // 4532 0151 1283 0366 es un número Visa válido por Luhn
        PagoRequestDTO request = new PagoRequestDTO(
                "mensual", "tarjeta_credito",
                "Enrique Prada", "4532015112830366", "12/28",
                "enrique.pdg@gmail.com"
        );

        SuscripcionDTO resultado = suscripcionService.pagar("enrique.pdg@gmail.com", request);

        assertThat(resultado.estado()).isEqualTo("activa");
        assertThat(resultado.monto()).isEqualByComparingTo("15.00");
        assertThat(resultado.tipoPlan()).isEqualTo("mensual");

        // El monto SIEMPRE sale del Plan en BD, nunca de lo que mande el cliente
        ArgumentCaptor<Pago> pagoCaptor = ArgumentCaptor.forClass(Pago.class);
        verify(pagoRepo).save(pagoCaptor.capture());
        assertThat(pagoCaptor.getValue().getMonto()).isEqualByComparingTo("15.00");
        assertThat(pagoCaptor.getValue().getUltimosCuatro()).isEqualTo("0366");
        assertThat(pagoCaptor.getValue().getPasarela()).isEqualTo("manual");

        ArgumentCaptor<Suscripcion> subCaptor = ArgumentCaptor.forClass(Suscripcion.class);
        verify(suscripcionRepo).save(subCaptor.capture());
        assertThat(subCaptor.getValue().getEstado()).isEqualTo("activa");
        assertThat(subCaptor.getValue().getFechaFin()).isEqualTo(LocalDate.now().plusMonths(1));
        assertThat(subCaptor.getValue().getAutoRenovar()).isTrue();

        // El paso crítico: el rol del usuario debe subir a premium
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepo).save(usuarioCaptor.capture());
        assertThat(usuarioCaptor.getValue().getRol().getNombre()).isEqualTo("usuario_premium");
    }

    @Test
    @DisplayName("pagar() plan anual calcula fecha_fin a un año y usa el precio del plan anual")
    void pagar_planAnual_calculaFechaYMontoCorrectos() {
        when(usuarioRepo.findByEmail(anyString())).thenReturn(Optional.of(usuarioFree));
        when(planRepo.findByNombre("Premium Anual")).thenReturn(Optional.of(planAnual));
        when(rolRepo.findByNombre("usuario_premium")).thenReturn(Optional.of(rolPremium));
        when(pagoRepo.save(any())).thenAnswer(inv -> { Pago p = inv.getArgument(0); p.setId(101); return p; });
        when(suscripcionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PagoRequestDTO request = new PagoRequestDTO(
                "anual", "yape", null, null, null, null
        );

        SuscripcionDTO resultado = suscripcionService.pagar("enrique.pdg@gmail.com", request);

        assertThat(resultado.tipoPlan()).isEqualTo("anual");
        assertThat(resultado.monto()).isEqualByComparingTo("120.00");
        assertThat(resultado.fechaFin()).isEqualTo(LocalDate.now().plusYears(1).toString());
    }

    // ═══════════════════════════════════════════════════
    // VALIDACIÓN — tarjeta inválida (falla Luhn) debe rechazarse
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("pagar() con número de tarjeta que falla Luhn debe lanzar excepción")
    void pagar_conTarjetaInvalida_debeRechazar() {
        when(usuarioRepo.findByEmail(anyString())).thenReturn(Optional.of(usuarioFree));
        when(planRepo.findByNombre("Premium Mensual")).thenReturn(Optional.of(planMensual));

        PagoRequestDTO request = new PagoRequestDTO(
                "mensual", "tarjeta_credito",
                "Enrique Prada", "1234567890123456", "12/28", null
        );

        assertThatThrownBy(() -> suscripcionService.pagar("enrique.pdg@gmail.com", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no es válido");

        verify(pagoRepo, never()).save(any());
        verify(usuarioRepo, never()).save(any());
    }

    @Test
    @DisplayName("pagar() con Yape no valida datos de tarjeta (no aplica)")
    void pagar_conYape_noValidaTarjeta() {
        when(usuarioRepo.findByEmail(anyString())).thenReturn(Optional.of(usuarioFree));
        when(planRepo.findByNombre("Premium Mensual")).thenReturn(Optional.of(planMensual));
        when(rolRepo.findByNombre("usuario_premium")).thenReturn(Optional.of(rolPremium));
        when(pagoRepo.save(any())).thenAnswer(inv -> { Pago p = inv.getArgument(0); p.setId(102); return p; });
        when(suscripcionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PagoRequestDTO request = new PagoRequestDTO("mensual", "yape", null, null, null, null);

        assertThatCode(() -> suscripcionService.pagar("enrique.pdg@gmail.com", request))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("pagar() con Yape deja auto_renovar en false (no existe débito automático real para Yape/Plin)")
    void pagar_conYape_autoRenovarQuedaFalse() {
        when(usuarioRepo.findByEmail(anyString())).thenReturn(Optional.of(usuarioFree));
        when(planRepo.findByNombre("Premium Mensual")).thenReturn(Optional.of(planMensual));
        when(rolRepo.findByNombre("usuario_premium")).thenReturn(Optional.of(rolPremium));
        when(pagoRepo.save(any())).thenAnswer(inv -> { Pago p = inv.getArgument(0); p.setId(103); return p; });
        when(suscripcionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PagoRequestDTO request = new PagoRequestDTO("mensual", "plin", null, null, null, null);

        SuscripcionDTO resultado = suscripcionService.pagar("enrique.pdg@gmail.com", request);
        assertThat(resultado.estado()).isEqualTo("activa");

        ArgumentCaptor<Suscripcion> subCaptor = ArgumentCaptor.forClass(Suscripcion.class);
        verify(suscripcionRepo).save(subCaptor.capture());
        assertThat(subCaptor.getValue().getAutoRenovar()).isFalse();
    }

    // ═══════════════════════════════════════════════════
    // RECONTRATAR — cancelada-vigente se reactiva, no se cobra de nuevo
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("pagar() con suscripción cancelada pero aún vigente la reactiva sin crear un pago nuevo")
    void pagar_conCanceladaVigente_reactivaSinCobrarDeNuevo() {
        Suscripcion canceladaVigente = new Suscripcion();
        canceladaVigente.setId(400);
        canceladaVigente.setEstado("cancelada");
        canceladaVigente.setFechaFin(LocalDate.now().plusDays(15));
        canceladaVigente.setFechaInicio(LocalDate.now().minusDays(15));
        canceladaVigente.setIdPagoOrigen(500);
        canceladaVigente.setPlan(planMensual);
        canceladaVigente.setPrecioPagado(new BigDecimal("15.00"));
        canceladaVigente.setMetodoPago("tarjeta_credito");

        Pago pagoOriginal = new Pago();
        pagoOriginal.setId(500);
        pagoOriginal.setRefInterna("111222");

        when(usuarioRepo.findByEmail("enrique.pdg@gmail.com")).thenReturn(Optional.of(usuarioFree));
        when(suscripcionRepo.findVigente(eq(10), any(LocalDate.class))).thenReturn(Optional.empty());
        when(suscripcionRepo.findCanceladaVigente(eq(10), any(LocalDate.class)))
                .thenReturn(Optional.of(canceladaVigente));
        when(suscripcionRepo.save(any(Suscripcion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rolRepo.findByNombre("usuario_premium")).thenReturn(Optional.of(rolPremium));
        when(pagoRepo.findById(500)).thenReturn(Optional.of(pagoOriginal));

        PagoRequestDTO request = new PagoRequestDTO("mensual", "tarjeta_credito",
                "Enrique Prada", "4532015112830366", "12/28", null);

        SuscripcionDTO resultado = suscripcionService.pagar("enrique.pdg@gmail.com", request);

        assertThat(resultado.estado()).isEqualTo("activa");
        assertThat(resultado.refInterna()).isEqualTo("111222");

        // No debe haberse creado ningún pago ni suscripción nueva
        verify(pagoRepo, never()).save(any());
        ArgumentCaptor<Suscripcion> captor = ArgumentCaptor.forClass(Suscripcion.class);
        verify(suscripcionRepo).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(400);
        assertThat(captor.getValue().getEstado()).isEqualTo("activa");

        // Pero el rol sí debe volver a premium
        verify(usuarioRepo).save(argThat(u -> "usuario_premium".equals(u.getRol().getNombre())));
    }

    @Test
    @DisplayName("pagar() al reactivar una cancelada-vigente actualiza auto_renovar según el método actual, no el viejo")
    void pagar_conCanceladaVigente_actualizaAutoRenovarSegunMetodoActual() {
        // La suscripción original se había pagado con tarjeta (auto_renovar habría sido true)
        Suscripcion canceladaVigente = new Suscripcion();
        canceladaVigente.setId(401);
        canceladaVigente.setEstado("cancelada");
        canceladaVigente.setFechaFin(LocalDate.now().plusDays(10));
        canceladaVigente.setFechaInicio(LocalDate.now().minusDays(20));
        canceladaVigente.setIdPagoOrigen(501);
        canceladaVigente.setPlan(planMensual);
        canceladaVigente.setPrecioPagado(new BigDecimal("15.00"));
        canceladaVigente.setMetodoPago("tarjeta_credito");
        canceladaVigente.setAutoRenovar(true);

        Pago pagoOriginal = new Pago();
        pagoOriginal.setId(501);
        pagoOriginal.setRefInterna("222333");

        when(usuarioRepo.findByEmail("enrique.pdg@gmail.com")).thenReturn(Optional.of(usuarioFree));
        when(suscripcionRepo.findVigente(eq(10), any(LocalDate.class))).thenReturn(Optional.empty());
        when(suscripcionRepo.findCanceladaVigente(eq(10), any(LocalDate.class)))
                .thenReturn(Optional.of(canceladaVigente));
        when(suscripcionRepo.save(any(Suscripcion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rolRepo.findByNombre("usuario_premium")).thenReturn(Optional.of(rolPremium));
        when(pagoRepo.findById(501)).thenReturn(Optional.of(pagoOriginal));

        // Esta vez reactiva pagando con Yape en vez de tarjeta
        PagoRequestDTO request = new PagoRequestDTO("mensual", "yape", null, null, null, null);

        suscripcionService.pagar("enrique.pdg@gmail.com", request);

        ArgumentCaptor<Suscripcion> captor = ArgumentCaptor.forClass(Suscripcion.class);
        verify(suscripcionRepo).save(captor.capture());
        assertThat(captor.getValue().getAutoRenovar()).isFalse();
    }

    @Test
    @DisplayName("pagar() con una suscripción activa vigente debe rechazar el doble cobro")
    void pagar_conActivaVigente_debeRechazar() {
        Suscripcion activaVigente = new Suscripcion();
        activaVigente.setId(600);
        activaVigente.setEstado("activa");
        activaVigente.setFechaFin(LocalDate.now().plusDays(10));

        when(usuarioRepo.findByEmail("enrique.pdg@gmail.com")).thenReturn(Optional.of(usuarioFree));
        when(suscripcionRepo.findVigente(eq(10), any(LocalDate.class))).thenReturn(Optional.of(activaVigente));

        PagoRequestDTO request = new PagoRequestDTO("mensual", "yape", null, null, null, null);

        assertThatThrownBy(() -> suscripcionService.pagar("enrique.pdg@gmail.com", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ya tienes una suscripción activa");

        verify(pagoRepo, never()).save(any());
        verify(suscripcionRepo, never()).save(any());
    }

    // ═══════════════════════════════════════════════════
    // RENOVAR ANTES DE VENCER — Yape/Plin activo a ≤7 días de vencer puede
    // adelantar el pago en vez de esperar a que corte el servicio.
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("pagar() con una suscripción activa a ≤7 días de vencer permite adelantar la renovación (extiende, no rechaza)")
    void pagar_conActivaAPocosDiasDeVencer_adelantaRenovacionSinRechazar() {
        Suscripcion activaPorVencer = new Suscripcion();
        activaPorVencer.setId(800);
        activaPorVencer.setEstado("activa");
        activaPorVencer.setFechaInicio(LocalDate.now().minusDays(27));
        activaPorVencer.setFechaFin(LocalDate.now().plusDays(3));
        activaPorVencer.setIdPagoOrigen(900);
        activaPorVencer.setPlan(planMensual);
        activaPorVencer.setPersona(persona);
        activaPorVencer.setMetodoPago("yape");
        activaPorVencer.setAutoRenovar(false);

        when(usuarioRepo.findByEmail("enrique.pdg@gmail.com")).thenReturn(Optional.of(usuarioFree));
        when(suscripcionRepo.findVigente(eq(10), any(LocalDate.class))).thenReturn(Optional.of(activaPorVencer));
        when(pagoRepo.save(any(Pago.class))).thenAnswer(inv -> { Pago p = inv.getArgument(0); p.setId(901); return p; });
        when(suscripcionRepo.save(any(Suscripcion.class))).thenAnswer(inv -> inv.getArgument(0));

        PagoRequestDTO request = new PagoRequestDTO("mensual", "plin", null, null, null, null);

        LocalDate fechaFinVieja = activaPorVencer.getFechaFin();
        SuscripcionDTO resultado = suscripcionService.pagar("enrique.pdg@gmail.com", request);

        assertThat(resultado.estado()).isEqualTo("activa");
        assertThat(resultado.fechaFin()).isEqualTo(fechaFinVieja.plusMonths(1).toString());

        // No crea una suscripcion nueva: extiende la misma (id=800)
        ArgumentCaptor<Suscripcion> subCaptor = ArgumentCaptor.forClass(Suscripcion.class);
        verify(suscripcionRepo).save(subCaptor.capture());
        assertThat(subCaptor.getValue().getId()).isEqualTo(800);
        assertThat(subCaptor.getValue().getIdPagoOrigen()).isEqualTo(901);

        // Sí crea un pago nuevo (el cobro del período adicional)
        verify(pagoRepo).save(any(Pago.class));

        // No se toca el rol (ya era premium, sigue premium sin corte)
        verify(usuarioRepo, never()).save(any());
    }

    // ═══════════════════════════════════════════════════
    // CANCELAR — marca cancelada pero NO degrada el rol al instante
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("cancelar() marca la suscripción como cancelada sin degradar el rol al instante")
    void cancelar_marcaCanceladaSinDegradarRolInstante() {
        Suscripcion vigente = new Suscripcion();
        vigente.setId(300);
        vigente.setEstado("activa");
        vigente.setFechaFin(LocalDate.now().plusDays(20));

        when(usuarioRepo.findByEmail("enrique.pdg@gmail.com")).thenReturn(Optional.of(usuarioFree));
        when(suscripcionRepo.findVigente(eq(10), any(LocalDate.class))).thenReturn(Optional.of(vigente));
        when(suscripcionRepo.save(any(Suscripcion.class))).thenAnswer(inv -> inv.getArgument(0));

        suscripcionService.cancelar("enrique.pdg@gmail.com");

        ArgumentCaptor<Suscripcion> captor = ArgumentCaptor.forClass(Suscripcion.class);
        verify(suscripcionRepo).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo("cancelada");

        // El rol NO se toca aquí — se degrada más adelante vía lazy expiry
        verify(usuarioRepo, never()).save(any());
    }

    @Test
    @DisplayName("cancelar() sin suscripción vigente debe lanzar excepción clara")
    void cancelar_sinSuscripcionVigente_debeLanzarExcepcion() {
        when(usuarioRepo.findByEmail(anyString())).thenReturn(Optional.of(usuarioFree));
        when(suscripcionRepo.findVigente(anyInt(), any(LocalDate.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> suscripcionService.cancelar("enrique.pdg@gmail.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No tienes una suscripción activa");
    }

}
