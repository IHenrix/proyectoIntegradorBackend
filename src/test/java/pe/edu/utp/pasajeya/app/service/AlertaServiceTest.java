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

@ExtendWith(MockitoExtension.class)
class AlertaServiceTest {

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

    @InjectMocks
    private AlertaServiceImpl alertaService;

    private Usuario usuarioFree;
    private Usuario usuarioPremium;
    private Vuelo vuelo;
    private Tarifa tarifa;
    private CrearAlertaRequestDTO requestValido;

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

        AlertaDTO resultado = alertaService.crear("ana@test.com", requestValido);

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
        Alerta alerta1 = construirAlerta(1, usuarioFree, vuelo, "basica", 200.0);
        Alerta alerta2 = construirAlerta(2, usuarioFree, vuelo, "flex", 300.0);
        when(alertaRepo.findByUsuarioEmailOrderByFechaCreacionDesc("ana@test.com"))
                .thenReturn(List.of(alerta1, alerta2));
        when(tarifaRepo.findFirstByVueloAndTipoOrderByPrecioAsc(any(), any()))
                .thenReturn(Optional.of(tarifa));

        List<AlertaDTO> resultado = alertaService.listar("ana@test.com");

        assertThat(resultado).hasSize(2);
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 3: Crear con tarifa inexistente debe lanzar excepción
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe lanzar excepcion al crear alerta con tarifa inexistente")
    void cuandoCreaConTarifaInexistente_debeLanzarExcepcion() {
        when(usuarioRepo.findByEmail("ana@test.com")).thenReturn(Optional.of(usuarioFree));
        when(tarifaRepo.findById(100)).thenReturn(Optional.empty());

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
        when(usuarioRepo.findByEmail("ana@test.com")).thenReturn(Optional.of(usuarioFree));
        when(tarifaRepo.findById(100)).thenReturn(Optional.of(tarifa));
        when(alertaRepo.existsByUsuarioEmailAndVueloIdAndTipoTarifa("ana@test.com", 10, "basica"))
                .thenReturn(false);
        when(alertaRepo.countByUsuarioEmail("ana@test.com")).thenReturn(3L);

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

        AlertaDTO resultado = alertaService.crear("premium@test.com", requestValido);

        assertThat(resultado).isNotNull();
        verify(alertaRepo, never()).countByUsuarioEmail(any());
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 6: Alerta duplicada debe lanzar excepcion
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe lanzar ALERTA_DUPLICADA si ya existe alerta para el mismo vuelo y tarifa")
    void cuandoAlertaYaExiste_debeLanzarAlertaDuplicada() {
        when(usuarioRepo.findByEmail("ana@test.com")).thenReturn(Optional.of(usuarioFree));
        when(tarifaRepo.findById(100)).thenReturn(Optional.of(tarifa));
        when(alertaRepo.existsByUsuarioEmailAndVueloIdAndTipoTarifa("ana@test.com", 10, "basica"))
                .thenReturn(true);

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
        Alerta alerta = construirAlerta(1, usuarioFree, vuelo, "basica", 200.0);
        when(alertaRepo.findByIdAndUsuarioEmail(1, "ana@test.com")).thenReturn(Optional.of(alerta));

        alertaService.eliminar("ana@test.com", 1);

        verify(alertaRepo, times(1)).delete(alerta);
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 8: Pausar una alerta cambia su estado activa a false
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe cambiar el estado activa a false al pausar una alerta")
    void cuandoPausarAlerta_debeCambiarEstadoActivaAFalse() {
        Alerta alerta = construirAlerta(1, usuarioFree, vuelo, "basica", 200.0);
        when(alertaRepo.findByIdAndUsuarioEmail(1, "ana@test.com")).thenReturn(Optional.of(alerta));
        when(tarifaRepo.findFirstByVueloAndTipoOrderByPrecioAsc(vuelo, "basica"))
                .thenReturn(Optional.of(tarifa));
        when(alertaRepo.save(any(Alerta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        alertaService.pausar("ana@test.com", 1);

        assertThat(alerta.getActiva()).isFalse();
        verify(alertaRepo, times(1)).save(alerta);
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 9: Crear alerta resolviendo tarifa por ruta completa
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe crear una alerta cuando se envia origen/destino/fecha en vez de tarifaId")
    void cuandoCreaPorRutaCompleta_debeResolverTarifaYCrearAlerta() {
        CrearAlertaRequestDTO requestPorRuta = new CrearAlertaRequestDTO(
                null, "LIM", "CUZ", "2026-07-01", "basica", 200.0, "987654321");

        when(usuarioRepo.findByEmail("ana@test.com")).thenReturn(Optional.of(usuarioFree));
        when(vueloRepo.findByOrigenAndDestinoAndFechaSalida("LIM", "CUZ", LocalDate.of(2026, 7, 1)))
                .thenReturn(List.of(vuelo));
        when(tarifaRepo.findFirstByVueloAndTipoOrderByPrecioAsc(vuelo, "basica"))
                .thenReturn(Optional.of(tarifa));
        when(alertaRepo.existsByUsuarioEmailAndVueloIdAndTipoTarifa("ana@test.com", 10, "basica"))
                .thenReturn(false);
        when(alertaRepo.countByUsuarioEmail("ana@test.com")).thenReturn(0L);
        when(alertaRepo.save(any(Alerta.class))).thenAnswer(invocation -> {
            Alerta a = invocation.getArgument(0);
            a.setId(2);
            return a;
        });

        AlertaDTO resultado = alertaService.crear("ana@test.com", requestPorRuta);

        assertThat(resultado).isNotNull();
        assertThat(resultado.tipoTarifa()).isEqualTo("basica");
        verify(alertaRepo, times(1)).save(any(Alerta.class));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 10: Sin vuelos disponibles para la ruta debe lanzar excepcion
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe lanzar excepcion cuando no hay vuelos disponibles para la ruta solicitada")
    void cuandoNoHayVuelosParaLaRuta_debeLanzarExcepcion() {
        CrearAlertaRequestDTO requestSinVuelos = new CrearAlertaRequestDTO(
                null, "LIM", "AQP", "2026-07-01", "basica", 200.0, "987654321");

        when(usuarioRepo.findByEmail("ana@test.com")).thenReturn(Optional.of(usuarioFree));
        when(vueloRepo.findByOrigenAndDestinoAndFechaSalida("LIM", "AQP", LocalDate.of(2026, 7, 1)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> alertaService.crear("ana@test.com", requestSinVuelos))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No hay vuelos disponibles");

        verify(alertaRepo, never()).save(any(Alerta.class));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 11: evaluarAlertasActivas notifica cuando el precio baja del objetivo
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe enviar notificacion cuando el precio actual esta por debajo del objetivo")
    void cuandoPrecioActualBajaDelObjetivo_debeEnviarNotificacion() {
        Alerta alerta = construirAlerta(1, usuarioFree, vuelo, "basica", 200.0);
        Tarifa tarifaBarata = new Tarifa();
        tarifaBarata.setId(101);
        tarifaBarata.setVuelo(vuelo);
        tarifaBarata.setTipo("basica");
        tarifaBarata.setPrecio(BigDecimal.valueOf(180.0));

        when(alertaRepo.findByActivaTrue()).thenReturn(List.of(alerta));
        when(tarifaRepo.findFirstByVueloAndTipoOrderByPrecioAsc(vuelo, "basica"))
                .thenReturn(Optional.of(tarifaBarata));
        when(historialRepo.calcularPromedio(eq(10), eq("basica"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(250.0));
        when(alertaRepo.save(any(Alerta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        alertaService.evaluarAlertasActivas();

        verify(whatsAppService, times(1)).enviarAlertaPrecio(eq(alerta), eq(BigDecimal.valueOf(180.0)), anyDouble());
        assertThat(alerta.getUltimoPrecioNotificado()).isEqualTo(BigDecimal.valueOf(180.0));
        verify(alertaRepo, times(1)).save(alerta);
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 12: evaluarAlertasActivas no notifica si el precio esta en rango normal
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("No debe notificar cuando el precio esta dentro del rango normal")
    void cuandoPrecioEnRangoNormal_noDebeNotificar() {
        Alerta alerta = construirAlerta(1, usuarioFree, vuelo, "basica", 200.0);
        Tarifa tarifaNormal = new Tarifa();
        tarifaNormal.setId(102);
        tarifaNormal.setVuelo(vuelo);
        tarifaNormal.setTipo("basica");
        tarifaNormal.setPrecio(BigDecimal.valueOf(245.0));

        when(alertaRepo.findByActivaTrue()).thenReturn(List.of(alerta));
        when(tarifaRepo.findFirstByVueloAndTipoOrderByPrecioAsc(vuelo, "basica"))
                .thenReturn(Optional.of(tarifaNormal));
        when(historialRepo.calcularPromedio(eq(10), eq("basica"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(250.0));

        alertaService.evaluarAlertasActivas();

        verify(whatsAppService, never()).enviarAlertaPrecio(any(), any(), anyDouble());
        verify(alertaRepo, never()).save(any(Alerta.class));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 13: evaluarAlertasActivas respeta el anti-spam (no renotifica)
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("No debe renotificar si el precio no bajo otro 10% desde la ultima notificacion")
    void cuandoYaNotificadoYPrecioNoBajaMas_noDebeRenotificar() {
        Alerta alerta = construirAlerta(1, usuarioFree, vuelo, "basica", 200.0);
        alerta.setUltimoPrecioNotificado(BigDecimal.valueOf(180.0));

        Tarifa tarifaIgual = new Tarifa();
        tarifaIgual.setId(103);
        tarifaIgual.setVuelo(vuelo);
        tarifaIgual.setTipo("basica");
        tarifaIgual.setPrecio(BigDecimal.valueOf(180.0));

        when(alertaRepo.findByActivaTrue()).thenReturn(List.of(alerta));
        when(tarifaRepo.findFirstByVueloAndTipoOrderByPrecioAsc(vuelo, "basica"))
                .thenReturn(Optional.of(tarifaIgual));
        when(historialRepo.calcularPromedio(eq(10), eq("basica"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(250.0));

        alertaService.evaluarAlertasActivas();

        verify(whatsAppService, never()).enviarAlertaPrecio(any(), any(), anyDouble());
        verify(alertaRepo, never()).save(any(Alerta.class));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 14: evaluarAlertasActivas ignora alertas sin tarifa disponible
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("No debe fallar ni notificar cuando la alerta no tiene tarifa disponible")
    void cuandoNoHayTarifaDisponible_noDebeNotificarNiFallar() {
        Alerta alerta = construirAlerta(1, usuarioFree, vuelo, "basica", 200.0);
        when(alertaRepo.findByActivaTrue()).thenReturn(List.of(alerta));
        when(tarifaRepo.findFirstByVueloAndTipoOrderByPrecioAsc(vuelo, "basica"))
                .thenReturn(Optional.empty());

        alertaService.evaluarAlertasActivas();

        verify(whatsAppService, never()).enviarAlertaPrecio(any(), any(), anyDouble());
        verify(historialRepo, never()).calcularPromedio(any(), any(), any());
    }

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
