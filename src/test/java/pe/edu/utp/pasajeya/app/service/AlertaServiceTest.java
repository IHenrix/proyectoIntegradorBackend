package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.AlertaDTO;
import pe.edu.utp.pasajeya.app.dto.CrearAlertaRequestDTO;
import pe.edu.utp.pasajeya.app.model.*;
import pe.edu.utp.pasajeya.app.repository.*;
import pe.edu.utp.pasajeya.app.service.impl.AlertaServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// 1) Activar Mockito
@ExtendWith(MockitoExtension.class)
class AlertaServiceTest {

    // 2) Mocks de todas las dependencias del servicio (6 en total)
    @Mock
    private AlertaRepository alertaRepo;
    @Mock
    private UsuarioRepository usuarioRepo;
    @Mock
    private VueloRepository vueloRepo;
    @Mock
    private TarifaRepository tarifaRepo;
    @Mock
    private HistorialPrecioRepository historialRepo;
    @Mock
    private WhatsAppNotificationService whatsAppService;

    // 3) Inyectar los mocks en el servicio bajo prueba
    @InjectMocks
    private AlertaServiceImpl alertaService;

    private Usuario usuarioFree;
    private Usuario usuarioPremium;
    private Vuelo vuelo;
    private Tarifa tarifa;
    private CrearAlertaRequestDTO requestValido;

    // 4) Preparar datos antes de cada prueba
    @BeforeEach
    void setUp() {
        Rol rolFree = new Rol();
        rolFree.setId(1);
        rolFree.setNombre("usuario_free");

        Rol rolPremium = new Rol();
        rolPremium.setId(2);
        rolPremium.setNombre("usuario_premium");

        usuarioFree = new Usuario();
        usuarioFree.setId(1);
        usuarioFree.setEmail("ana@test.com");
        usuarioFree.setRol(rolFree);

        usuarioPremium = new Usuario();
        usuarioPremium.setId(2);
        usuarioPremium.setEmail("premium@test.com");
        usuarioPremium.setRol(rolPremium);

        Aerolinea aerolinea = new Aerolinea();
        aerolinea.setNombre("LATAM Airlines");

        vuelo = new Vuelo();
        vuelo.setId(10);
        vuelo.setAerolinea(aerolinea);
        vuelo.setOrigen("LIM");
        vuelo.setDestino("CUZ");
        vuelo.setFechaSalida(LocalDate.of(2026, 7, 1));
        vuelo.setHoraSalida(LocalTime.of(8, 30));
        vuelo.setDuracionMin(80);

        tarifa = new Tarifa();
        tarifa.setId(100);
        tarifa.setVuelo(vuelo);
        tarifa.setTipo("basica");
        tarifa.setPrecio(BigDecimal.valueOf(250.0));

        requestValido = new CrearAlertaRequestDTO(
                100L, null, null, null, null, 200.0, "987654321"
        );
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 1: Crear alerta correctamente
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe crear una alerta y retornar el DTO guardado")
    void cuandoCreaAlerta_debeRetornarAlertaGuardada() {
        // GIVEN
        when(usuarioRepo.findByEmail("ana@test.com")).thenReturn(Optional.of(usuarioFree));
        when(tarifaRepo.findById(100)).thenReturn(Optional.of(tarifa));
        when(alertaRepo.existsByUsuarioEmailAndVueloIdAndTipoTarifa("ana@test.com", 10, "basica"))
                .thenReturn(false);
        when(alertaRepo.countByUsuarioEmail("ana@test.com")).thenReturn(0L);
        when(tarifaRepo.findFirstByVueloAndTipoOrderByPrecioAsc(vuelo, "basica"))
                .thenReturn(Optional.of(tarifa));
        when(alertaRepo.save(any(Alerta.class))).thenAnswer(invocation -> {
            Alerta a = invocation.getArgument(0);
            a.setId(1);
            return a;
        });

        // WHEN
        AlertaDTO resultado = alertaService.crear("ana@test.com", requestValido);

        // THEN
        assertThat(resultado).isNotNull();
        assertThat(resultado.precioObjetivo()).isEqualTo(200.0);
        assertThat(resultado.telefono()).isEqualTo("987654321");
        verify(alertaRepo, times(1)).save(any(Alerta.class));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 2: Listar alertas de un usuario
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe retornar la lista de alertas del usuario")
    void cuandoLista_debeRetornarListaDeAlertas() {
        // GIVEN
        Alerta alerta1 = construirAlerta(1, usuarioFree, vuelo, "basica", 200.0);
        Alerta alerta2 = construirAlerta(2, usuarioFree, vuelo, "flex", 300.0);
        when(alertaRepo.findByUsuarioEmailOrderByFechaCreacionDesc("ana@test.com"))
                .thenReturn(List.of(alerta1, alerta2));
        when(tarifaRepo.findFirstByVueloAndTipoOrderByPrecioAsc(any(), any()))
                .thenReturn(Optional.of(tarifa));

        // WHEN
        List<AlertaDTO> resultado = alertaService.listar("ana@test.com");

        // THEN
        assertThat(resultado).hasSize(2);
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 3: Crear con tarifa inexistente debe lanzar excepción
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe lanzar excepcion al crear alerta con tarifa inexistente")
    void cuandoCreaConTarifaInexistente_debeLanzarExcepcion() {
        // GIVEN
        when(usuarioRepo.findByEmail("ana@test.com")).thenReturn(Optional.of(usuarioFree));
        when(tarifaRepo.findById(100)).thenReturn(Optional.empty());

        // WHEN + THEN
        assertThatThrownBy(() -> alertaService.crear("ana@test.com", requestValido))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tarifa no encontrada");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 4: Usuario free que supera el limite de 3 alertas
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe lanzar LIMITE_ALERTAS cuando un usuario free ya tiene 3 alertas")
    void cuandoUsuarioFreeSuperaLimite_debeLanzarLimiteAlertas() {
        // GIVEN
        when(usuarioRepo.findByEmail("ana@test.com")).thenReturn(Optional.of(usuarioFree));
        when(tarifaRepo.findById(100)).thenReturn(Optional.of(tarifa));
        when(alertaRepo.existsByUsuarioEmailAndVueloIdAndTipoTarifa("ana@test.com", 10, "basica"))
                .thenReturn(false);
        when(alertaRepo.countByUsuarioEmail("ana@test.com")).thenReturn(3L);

        // WHEN + THEN
        assertThatThrownBy(() -> alertaService.crear("ana@test.com", requestValido))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("LIMITE_ALERTAS");

        verify(alertaRepo, never()).save(any(Alerta.class));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 5: Usuario premium no esta limitado a 3 alertas
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Un usuario premium debe poder crear su cuarta alerta sin error")
    void cuandoUsuarioPremium_noDebeAplicarLimite() {
        // GIVEN
        when(usuarioRepo.findByEmail("premium@test.com")).thenReturn(Optional.of(usuarioPremium));
        when(tarifaRepo.findById(100)).thenReturn(Optional.of(tarifa));
        when(alertaRepo.existsByUsuarioEmailAndVueloIdAndTipoTarifa("premium@test.com", 10, "basica"))
                .thenReturn(false);
        when(tarifaRepo.findFirstByVueloAndTipoOrderByPrecioAsc(vuelo, "basica"))
                .thenReturn(Optional.of(tarifa));
        when(alertaRepo.save(any(Alerta.class))).thenAnswer(invocation -> {
            Alerta a = invocation.getArgument(0);
            a.setId(5);
            return a;
        });

        // WHEN
        AlertaDTO resultado = alertaService.crear("premium@test.com", requestValido);

        // THEN
        assertThat(resultado).isNotNull();
        verify(alertaRepo, never()).countByUsuarioEmail(any());
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 6: Alerta duplicada debe lanzar excepcion
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe lanzar ALERTA_DUPLICADA si ya existe alerta para el mismo vuelo y tarifa")
    void cuandoAlertaYaExiste_debeLanzarAlertaDuplicada() {
        // GIVEN
        when(usuarioRepo.findByEmail("ana@test.com")).thenReturn(Optional.of(usuarioFree));
        when(tarifaRepo.findById(100)).thenReturn(Optional.of(tarifa));
        when(alertaRepo.existsByUsuarioEmailAndVueloIdAndTipoTarifa("ana@test.com", 10, "basica"))
                .thenReturn(true);

        // WHEN + THEN
        assertThatThrownBy(() -> alertaService.crear("ana@test.com", requestValido))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ALERTA_DUPLICADA");

        verify(alertaRepo, never()).save(any(Alerta.class));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 7: Eliminar alerta invoca delete exactamente una vez
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe invocar delete en el repositorio al eliminar una alerta")
    void cuandoElimina_debeInvocarDeleteUnaVez() {
        // GIVEN
        Alerta alerta = construirAlerta(1, usuarioFree, vuelo, "basica", 200.0);
        when(alertaRepo.findByIdAndUsuarioEmail(1, "ana@test.com")).thenReturn(Optional.of(alerta));

        // WHEN
        alertaService.eliminar("ana@test.com", 1);

        // THEN
        verify(alertaRepo, times(1)).delete(alerta);
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 8: Pausar una alerta cambia su estado activa a false
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe cambiar el estado activa a false al pausar una alerta")
    void cuandoPausarAlerta_debeCambiarEstadoActivaAFalse() {
        // GIVEN
        Alerta alerta = construirAlerta(1, usuarioFree, vuelo, "basica", 200.0);
        when(alertaRepo.findByIdAndUsuarioEmail(1, "ana@test.com")).thenReturn(Optional.of(alerta));
        when(tarifaRepo.findFirstByVueloAndTipoOrderByPrecioAsc(vuelo, "basica"))
                .thenReturn(Optional.of(tarifa));
        when(alertaRepo.save(any(Alerta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        alertaService.pausar("ana@test.com", 1);

        // THEN
        assertThat(alerta.getActiva()).isFalse();
        verify(alertaRepo, times(1)).save(alerta);
    }

    // ═══════════════════════════════════════════════════
    // Helper para construir alertas de prueba
    // ═══════════════════════════════════════════════════
    private Alerta construirAlerta(Integer id, Usuario usuario, Vuelo vuelo, String tipoTarifa, double precioObjetivo) {
        Alerta alerta = new Alerta();
        alerta.setId(id);
        alerta.setUsuario(usuario);
        alerta.setVuelo(vuelo);
        alerta.setTipoTarifa(tipoTarifa);
        alerta.setPrecioObjetivo(BigDecimal.valueOf(precioObjetivo));
        alerta.setTelefono("987654321");
        alerta.setActiva(true);
        alerta.setFechaCreacion(LocalDateTime.now());
        return alerta;
    }
}
