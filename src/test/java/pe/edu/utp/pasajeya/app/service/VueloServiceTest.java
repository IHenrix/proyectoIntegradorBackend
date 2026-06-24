package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.VueloDTO;
import pe.edu.utp.pasajeya.app.model.*;
import pe.edu.utp.pasajeya.app.repository.HistorialPrecioRepository;
import pe.edu.utp.pasajeya.app.repository.TarifaRepository;
import pe.edu.utp.pasajeya.app.repository.VueloRepository;
import pe.edu.utp.pasajeya.app.service.impl.VueloServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VueloServiceTest {

    @Mock
    private VueloRepository vueloRepo;
    @Mock
    private TarifaRepository tarifaRepo;
    @Mock
    private HistorialPrecioRepository historialRepo;

    @InjectMocks
    private VueloServiceImpl vueloService;

    private Vuelo vuelo;
    private Tarifa tarifaBasica;

    @BeforeEach
    void setUp() {
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

        tarifaBasica = new Tarifa();
        tarifaBasica.setId(100);
        tarifaBasica.setVuelo(vuelo);
        tarifaBasica.setTipo("basica");
        tarifaBasica.setPrecio(BigDecimal.valueOf(250.0));
        tarifaBasica.setEquipajeBodegaKg(0);
        tarifaBasica.setEquipajeManoKg(8);
        tarifaBasica.setPermiteReembolso(false);
        tarifaBasica.setAsientoSeleccionable(false);
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 1: Sin vuelos disponibles retorna lista vacia
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe retornar lista vacia cuando no hay vuelos para la ruta")
    void cuandoNoHayVuelos_debeRetornarListaVacia() {
        when(vueloRepo.findByOrigenAndDestinoAndFechaSalida("LIM", "CUZ", LocalDate.of(2026, 7, 1)))
                .thenReturn(List.of());

        List<VueloDTO> resultado = vueloService.buscarVuelos("LIM", "CUZ", "2026-07-01", 1);

        assertThat(resultado).isEmpty();
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 2: Mapea correctamente vuelo + tarifa a VueloDTO
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe mapear vuelo y tarifa a VueloDTO con los datos correctos")
    void cuandoHayVuelos_debeMapearCorrectamente() {
        when(vueloRepo.findByOrigenAndDestinoAndFechaSalida("LIM", "CUZ", LocalDate.of(2026, 7, 1)))
                .thenReturn(List.of(vuelo));
        when(tarifaRepo.findByVueloOrderByPrecioAsc(vuelo)).thenReturn(List.of(tarifaBasica));
        when(historialRepo.calcularPromedio(eq(10), eq("basica"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        List<VueloDTO> resultado = vueloService.buscarVuelos("LIM", "CUZ", "2026-07-01", 1);

        assertThat(resultado).hasSize(1);
        VueloDTO dto = resultado.get(0);
        assertThat(dto.aerolinea()).isEqualTo("LATAM");
        assertThat(dto.origen()).isEqualTo("LIM");
        assertThat(dto.destino()).isEqualTo("CUZ");
        assertThat(dto.horaSalida()).isEqualTo("08:30");
        assertThat(dto.horaLlegada()).isEqualTo("09:50");
        assertThat(dto.duracion()).isEqualTo("1h 20m");
        assertThat(dto.precio()).isEqualTo(250.0);
        assertThat(dto.incluyeEquipaje()).isFalse();
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 3: Ordena el resultado final por precio ascendente
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe ordenar las tarifas resultantes por precio ascendente")
    void cuandoHayVariasTarifas_debeOrdenarPorPrecio() {
        Tarifa tarifaCara = new Tarifa();
        tarifaCara.setId(101);
        tarifaCara.setVuelo(vuelo);
        tarifaCara.setTipo("flex");
        tarifaCara.setPrecio(BigDecimal.valueOf(400.0));
        tarifaCara.setEquipajeBodegaKg(23);
        tarifaCara.setEquipajeManoKg(8);
        tarifaCara.setPermiteReembolso(true);
        tarifaCara.setAsientoSeleccionable(true);

        when(vueloRepo.findByOrigenAndDestinoAndFechaSalida("LIM", "CUZ", LocalDate.of(2026, 7, 1)))
                .thenReturn(List.of(vuelo));
        when(tarifaRepo.findByVueloOrderByPrecioAsc(vuelo)).thenReturn(List.of(tarifaBasica, tarifaCara));
        when(historialRepo.calcularPromedio(any(), any(), any())).thenReturn(Optional.empty());

        List<VueloDTO> resultado = vueloService.buscarVuelos("LIM", "CUZ", "2026-07-01", 1);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).precio()).isEqualTo(250.0);
        assertThat(resultado.get(1).precio()).isEqualTo(400.0);
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 4: Semaforo verde cuando el precio esta bien por debajo del promedio
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe asignar semaforo verde cuando el precio esta por debajo del promedio")
    void cuandoPrecioMuyBajo_debeAsignarSemaforoVerde() {
        when(vueloRepo.findByOrigenAndDestinoAndFechaSalida("LIM", "CUZ", LocalDate.of(2026, 7, 1)))
                .thenReturn(List.of(vuelo));
        when(tarifaRepo.findByVueloOrderByPrecioAsc(vuelo)).thenReturn(List.of(tarifaBasica));
        when(historialRepo.calcularPromedio(eq(10), eq("basica"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(400.0));

        List<VueloDTO> resultado = vueloService.buscarVuelos("LIM", "CUZ", "2026-07-01", 1);

        assertThat(resultado.get(0).semaforo()).isEqualTo("verde");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 5: Semaforo rojo cuando el precio esta muy por encima del promedio
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe asignar semaforo rojo cuando el precio esta por encima del promedio")
    void cuandoPrecioMuyAlto_debeAsignarSemaforoRojo() {
        when(vueloRepo.findByOrigenAndDestinoAndFechaSalida("LIM", "CUZ", LocalDate.of(2026, 7, 1)))
                .thenReturn(List.of(vuelo));
        when(tarifaRepo.findByVueloOrderByPrecioAsc(vuelo)).thenReturn(List.of(tarifaBasica));
        when(historialRepo.calcularPromedio(eq(10), eq("basica"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(200.0));

        List<VueloDTO> resultado = vueloService.buscarVuelos("LIM", "CUZ", "2026-07-01", 1);

        assertThat(resultado.get(0).semaforo()).isEqualTo("rojo");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 6: Semaforo amarillo cuando no hay historial de precios
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe asignar semaforo amarillo cuando no hay historial de precios")
    void cuandoNoHayHistorial_debeAsignarSemaforoAmarillo() {
        when(vueloRepo.findByOrigenAndDestinoAndFechaSalida("LIM", "CUZ", LocalDate.of(2026, 7, 1)))
                .thenReturn(List.of(vuelo));
        when(tarifaRepo.findByVueloOrderByPrecioAsc(vuelo)).thenReturn(List.of(tarifaBasica));
        when(historialRepo.calcularPromedio(eq(10), eq("basica"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        List<VueloDTO> resultado = vueloService.buscarVuelos("LIM", "CUZ", "2026-07-01", 1);

        assertThat(resultado.get(0).semaforo()).isEqualTo("amarillo");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 7: Genera URL de LATAM con los parametros correctos
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe generar la URL de LATAM con origen, destino, fecha y pasajeros")
    void cuandoAerolineaEsLatam_debeGenerarUrlCorrecta() {
        when(vueloRepo.findByOrigenAndDestinoAndFechaSalida("LIM", "CUZ", LocalDate.of(2026, 7, 1)))
                .thenReturn(List.of(vuelo));
        when(tarifaRepo.findByVueloOrderByPrecioAsc(vuelo)).thenReturn(List.of(tarifaBasica));
        when(historialRepo.calcularPromedio(any(), any(), any())).thenReturn(Optional.empty());

        List<VueloDTO> resultado = vueloService.buscarVuelos("LIM", "CUZ", "2026-07-01", 2);

        assertThat(resultado.get(0).urlAerolinea())
                .contains("latamairlines.com")
                .contains("origin=LIM")
                .contains("destination=CUZ")
                .contains("adt=2");
    }
}
