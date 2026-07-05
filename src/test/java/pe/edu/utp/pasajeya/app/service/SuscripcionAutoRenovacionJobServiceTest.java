package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.model.Pago;
import pe.edu.utp.pasajeya.app.model.Persona;
import pe.edu.utp.pasajeya.app.model.Plan;
import pe.edu.utp.pasajeya.app.model.Suscripcion;
import pe.edu.utp.pasajeya.app.repository.PagoRepository;
import pe.edu.utp.pasajeya.app.repository.SuscripcionRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Verifica el job de auto-renovación de tarjeta: debe cobrar de nuevo (nuevo
 * Pago reusando marca/últimos4/titular del pago origen) y extender fecha_fin
 * a partir del valor VIEJO de fecha_fin — nunca desde "hoy" — para que un
 * retraso del scheduler no le regale ni le quite días al usuario.
 */
@ExtendWith(MockitoExtension.class)
class SuscripcionAutoRenovacionJobServiceTest {

    @Mock private SuscripcionRepository suscripcionRepo;
    @Mock private PagoRepository pagoRepo;

    @InjectMocks
    private SuscripcionAutoRenovacionJobService jobService;

    private Persona persona;
    private Plan planMensual;
    private Plan planAnual;
    private Pago pagoOrigen;

    @BeforeEach
    void setUp() {
        persona = new Persona();
        persona.setId(10);

        planMensual = new Plan();
        planMensual.setId(2);
        planMensual.setNombre("Premium Mensual");
        planMensual.setPrecioMensual(new BigDecimal("15.00"));
        planMensual.setDuracionDias(30);

        planAnual = new Plan();
        planAnual.setId(3);
        planAnual.setNombre("Premium Anual");
        planAnual.setPrecioMensual(new BigDecimal("120.00"));
        planAnual.setDuracionDias(365);

        pagoOrigen = new Pago();
        pagoOrigen.setId(500);
        pagoOrigen.setUltimosCuatro("0366");
        pagoOrigen.setMarcaTarjeta("visa");
        pagoOrigen.setTitularTarjeta("Enrique Prada");
        pagoOrigen.setEmailRecibo("enrique.pdg@gmail.com");
    }

    @Test
    @DisplayName("renovarAutomaticas() extiende fecha_fin desde el valor VIEJO, no desde hoy")
    void renovarAutomaticas_extiendeDesdeFechaFinVieja_noDesdeHoy() {
        LocalDate fechaFinVieja = LocalDate.now().minusDays(5); // venció hace 5 días (job con retraso)

        Suscripcion vencida = new Suscripcion();
        vencida.setId(700);
        vencida.setPersona(persona);
        vencida.setPlan(planMensual);
        vencida.setEstado("activa");
        vencida.setAutoRenovar(true);
        vencida.setFechaFin(fechaFinVieja);
        vencida.setIdPagoOrigen(500);
        vencida.setMetodoPago("tarjeta_credito");

        when(suscripcionRepo.findParaAutoRenovar(any(LocalDate.class))).thenReturn(List.of(vencida));
        when(pagoRepo.findById(500)).thenReturn(Optional.of(pagoOrigen));
        when(pagoRepo.save(any(Pago.class))).thenAnswer(inv -> { Pago p = inv.getArgument(0); p.setId(999); return p; });
        when(suscripcionRepo.save(any(Suscripcion.class))).thenAnswer(inv -> inv.getArgument(0));

        jobService.renovarAutomaticas();

        ArgumentCaptor<Suscripcion> subCaptor = ArgumentCaptor.forClass(Suscripcion.class);
        verify(suscripcionRepo).save(subCaptor.capture());
        assertThat(subCaptor.getValue().getFechaFin()).isEqualTo(fechaFinVieja.plusMonths(1));
        assertThat(subCaptor.getValue().getIdPagoOrigen()).isEqualTo(999);

        ArgumentCaptor<Pago> pagoCaptor = ArgumentCaptor.forClass(Pago.class);
        verify(pagoRepo).save(pagoCaptor.capture());
        assertThat(pagoCaptor.getValue().getUltimosCuatro()).isEqualTo("0366");
        assertThat(pagoCaptor.getValue().getMarcaTarjeta()).isEqualTo("visa");
        assertThat(pagoCaptor.getValue().getTitularTarjeta()).isEqualTo("Enrique Prada");
        assertThat(pagoCaptor.getValue().getMonto()).isEqualByComparingTo("15.00");
        assertThat(pagoCaptor.getValue().getPasarela()).isEqualTo("manual");
    }

    @Test
    @DisplayName("renovarAutomaticas() plan anual extiende un año, no un mes")
    void renovarAutomaticas_planAnual_extiendeUnAnio() {
        LocalDate fechaFinVieja = LocalDate.now();

        Suscripcion vencida = new Suscripcion();
        vencida.setId(701);
        vencida.setPersona(persona);
        vencida.setPlan(planAnual);
        vencida.setEstado("activa");
        vencida.setAutoRenovar(true);
        vencida.setFechaFin(fechaFinVieja);
        vencida.setIdPagoOrigen(500);
        vencida.setMetodoPago("tarjeta_debito");

        when(suscripcionRepo.findParaAutoRenovar(any(LocalDate.class))).thenReturn(List.of(vencida));
        when(pagoRepo.findById(500)).thenReturn(Optional.of(pagoOrigen));
        when(pagoRepo.save(any(Pago.class))).thenAnswer(inv -> { Pago p = inv.getArgument(0); p.setId(998); return p; });
        when(suscripcionRepo.save(any(Suscripcion.class))).thenAnswer(inv -> inv.getArgument(0));

        jobService.renovarAutomaticas();

        ArgumentCaptor<Suscripcion> subCaptor = ArgumentCaptor.forClass(Suscripcion.class);
        verify(suscripcionRepo).save(subCaptor.capture());
        assertThat(subCaptor.getValue().getFechaFin()).isEqualTo(fechaFinVieja.plusYears(1));
    }

    @Test
    @DisplayName("renovarAutomaticas() sin nada para renovar no crea pagos ni guarda suscripciones")
    void renovarAutomaticas_sinNadaQueRenovar_noHaceNada() {
        when(suscripcionRepo.findParaAutoRenovar(any(LocalDate.class))).thenReturn(List.of());

        jobService.renovarAutomaticas();

        verify(pagoRepo, never()).save(any());
        verify(suscripcionRepo, never()).save(any());
    }
}
