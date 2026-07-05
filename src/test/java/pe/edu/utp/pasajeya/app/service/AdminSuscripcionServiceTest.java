package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.AdminPagoDTO;
import pe.edu.utp.pasajeya.app.dto.AdminSuscripcionDTO;
import pe.edu.utp.pasajeya.app.model.*;
import pe.edu.utp.pasajeya.app.repository.PagoRepository;
import pe.edu.utp.pasajeya.app.repository.SuscripcionRepository;
import pe.edu.utp.pasajeya.app.repository.UsuarioRepository;
import pe.edu.utp.pasajeya.app.service.impl.AdminSuscripcionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSuscripcionServiceTest {

    @Mock private SuscripcionRepository suscripcionRepo;
    @Mock private PagoRepository pagoRepo;
    @Mock private UsuarioRepository usuarioRepo;

    @InjectMocks
    private AdminSuscripcionServiceImpl adminSuscripcionService;

    private Persona persona;
    private Usuario usuario;
    private Plan planMensual;

    @BeforeEach
    void setUp() {
        persona = new Persona();
        persona.setId(20);
        persona.setNombre("Enrique");
        persona.setApellidoPaterno("Prada");

        Rol rolPremium = new Rol();
        rolPremium.setNombre("usuario_premium");

        usuario = new Usuario();
        usuario.setEmail("enrique.pdg@gmail.com");
        usuario.setRol(rolPremium);
        usuario.setPersona(persona);

        planMensual = new Plan();
        planMensual.setNombre("Premium Mensual");
        planMensual.setDuracionDias(30);
    }

    @Test
    @DisplayName("listarSuscripciones() incluye email y nombre del titular resueltos por persona.id")
    void listarSuscripciones_incluyeDatosDelTitular() {
        Suscripcion sub = new Suscripcion();
        sub.setId(100);
        sub.setPersona(persona);
        sub.setPlan(planMensual);
        sub.setPrecioPagado(new BigDecimal("19.00"));
        sub.setFechaInicio(LocalDate.of(2026, 1, 1));
        sub.setFechaFin(LocalDate.of(2026, 2, 1));
        sub.setEstado("activa");
        sub.setMetodoPago("tarjeta_credito");
        sub.setAutoRenovar(true);

        when(suscripcionRepo.findAllByOrderByFechaInicioDesc()).thenReturn(List.of(sub));
        when(usuarioRepo.findByPersonaId(20)).thenReturn(Optional.of(usuario));

        List<AdminSuscripcionDTO> resultado = adminSuscripcionService.listarSuscripciones();

        assertThat(resultado).hasSize(1);
        AdminSuscripcionDTO dto = resultado.get(0);
        assertThat(dto.emailUsuario()).isEqualTo("enrique.pdg@gmail.com");
        assertThat(dto.nombreUsuario()).isEqualTo("Enrique Prada");
        assertThat(dto.tipoPlan()).isEqualTo("mensual");
        assertThat(dto.autoRenovar()).isTrue();
    }

    @Test
    @DisplayName("listarSuscripciones() con usuario no resuelto no lanza excepción, usa placeholder")
    void listarSuscripciones_usuarioNoEncontrado_usaPlaceholder() {
        Suscripcion sub = new Suscripcion();
        sub.setId(101);
        sub.setPersona(persona);
        sub.setPlan(planMensual);
        sub.setPrecioPagado(new BigDecimal("19.00"));
        sub.setFechaInicio(LocalDate.of(2026, 1, 1));
        sub.setFechaFin(LocalDate.of(2026, 2, 1));
        sub.setEstado("activa");
        sub.setMetodoPago("yape");
        sub.setAutoRenovar(false);

        when(suscripcionRepo.findAllByOrderByFechaInicioDesc()).thenReturn(List.of(sub));
        when(usuarioRepo.findByPersonaId(20)).thenReturn(Optional.empty());

        List<AdminSuscripcionDTO> resultado = adminSuscripcionService.listarSuscripciones();

        assertThat(resultado.get(0).emailUsuario()).isEqualTo("—");
        assertThat(resultado.get(0).nombreUsuario()).isEqualTo("—");
    }

    @Test
    @DisplayName("listarPagos() incluye email y nombre del titular resueltos por persona.id")
    void listarPagos_incluyeDatosDelTitular() {
        Pago pago = new Pago();
        pago.setId(200);
        pago.setPersona(persona);
        pago.setMonto(new BigDecimal("19.00"));
        pago.setMoneda("PEN");
        pago.setMetodo("tarjeta_credito");
        pago.setEstado("aprobado");
        pago.setRefInterna("123456");
        pago.setFechaPago(LocalDateTime.of(2026, 1, 1, 10, 0));

        when(pagoRepo.findAllByOrderByFechaPagoDesc()).thenReturn(List.of(pago));
        when(usuarioRepo.findByPersonaId(20)).thenReturn(Optional.of(usuario));

        List<AdminPagoDTO> resultado = adminSuscripcionService.listarPagos();

        assertThat(resultado).hasSize(1);
        AdminPagoDTO dto = resultado.get(0);
        assertThat(dto.emailUsuario()).isEqualTo("enrique.pdg@gmail.com");
        assertThat(dto.nombreUsuario()).isEqualTo("Enrique Prada");
        assertThat(dto.refInterna()).isEqualTo("123456");
        assertThat(dto.monto()).isEqualByComparingTo("19.00");
    }
}
