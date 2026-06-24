package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.VueloDetalleDTO;
import pe.edu.utp.pasajeya.app.model.*;
import pe.edu.utp.pasajeya.app.repository.HistorialPrecioRepository;
import pe.edu.utp.pasajeya.app.repository.TarifaRepository;
import pe.edu.utp.pasajeya.app.service.impl.VueloDetalleServiceImpl;
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
class VueloDetalleServiceTest {

    @Mock
    private TarifaRepository tarifaRepo;
    @Mock
    private HistorialPrecioRepository historialRepo;

    @InjectMocks
    private VueloDetalleServiceImpl detalleService;

    private Vuelo vuelo;
    private Tarifa tarifa;

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

        tarifa = new Tarifa();
        tarifa.setId(100);
        tarifa.setVuelo(vuelo);
        tarifa.setTipo("basica");
        tarifa.setPrecio(BigDecimal.valueOf(250.0));
        tarifa.setEquipajeBodegaKg(0);
        tarifa.setEquipajeManoKg(8);
        tarifa.setPermiteReembolso(false);
        tarifa.setAsientoSeleccionable(false);
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 1: Tarifa inexistente debe lanzar excepcion
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe lanzar excepcion cuando la tarifa no existe")
    void cuandoTarifaNoExiste_debeLanzarExcepcion() {
        when(tarifaRepo.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> detalleService.obtenerDetalle(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tarifa no encontrada");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 2: Sin historial de precios, la prediccion usa slope = 0
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe predecir precios constantes cuando no hay historial")
    void cuandoNoHayHistorial_debePredecirPreciosConstantes() {
        when(tarifaRepo.findById(100)).thenReturn(Optional.of(tarifa));
        when(historialRepo.findByVueloIdAndTipoTarifaAndFechaCapturaGreaterThanEqualOrderByFechaCapturaAsc(
                eq(10), eq("basica"), any(LocalDateTime.class))).thenReturn(List.of());

        VueloDetalleDTO resultado = detalleService.obtenerDetalle(100L);

        assertThat(resultado.prediccion()).hasSize(7);
        assertThat(resultado.prediccion().get(0).precioEstimado()).isEqualTo(250.0);
        assertThat(resultado.prediccion().get(6).precioEstimado()).isEqualTo(250.0);
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 3: Historial con tendencia creciente predice precios al alza
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe predecir tendencia al alza cuando el historial es creciente")
    void cuandoHistorialEsCreciente_debePredecirTendenciaAlAlza() {
        List<HistorialPrecio> historial = List.of(
                crearHistorial("2026-06-01", 200.0),
                crearHistorial("2026-06-02", 220.0),
                crearHistorial("2026-06-03", 240.0)
        );
        when(tarifaRepo.findById(100)).thenReturn(Optional.of(tarifa));
        when(historialRepo.findByVueloIdAndTipoTarifaAndFechaCapturaGreaterThanEqualOrderByFechaCapturaAsc(
                eq(10), eq("basica"), any(LocalDateTime.class))).thenReturn(historial);

        VueloDetalleDTO resultado = detalleService.obtenerDetalle(100L);

        double ultimoEstimado = resultado.prediccion().get(6).precioEstimado();
        assertThat(ultimoEstimado).isGreaterThan(tarifa.getPrecio().doubleValue());
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 4: Recomienda "Comprar ahora" cuando el precio esta muy debajo del promedio
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe recomendar comprar ahora cuando el precio esta muy por debajo del promedio")
    void cuandoPrecioMuyBajo_debeRecomendarComprarAhora() {
        List<HistorialPrecio> historial = List.of(
                crearHistorial("2026-06-01", 400.0),
                crearHistorial("2026-06-02", 400.0)
        );
        when(tarifaRepo.findById(100)).thenReturn(Optional.of(tarifa));
        when(historialRepo.findByVueloIdAndTipoTarifaAndFechaCapturaGreaterThanEqualOrderByFechaCapturaAsc(
                eq(10), eq("basica"), any(LocalDateTime.class))).thenReturn(historial);

        VueloDetalleDTO resultado = detalleService.obtenerDetalle(100L);

        assertThat(resultado.recomendacion()).startsWith("Comprar ahora");
        assertThat(resultado.semaforo()).isEqualTo("verde");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 5: Recomienda "Monitorear" cuando el precio esta cerca del promedio
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe recomendar monitorear cuando el precio esta cerca del promedio")
    void cuandoPrecioNormal_debeRecomendarMonitorear() {
        List<HistorialPrecio> historial = List.of(
                crearHistorial("2026-06-01", 250.0),
                crearHistorial("2026-06-02", 250.0)
        );
        when(tarifaRepo.findById(100)).thenReturn(Optional.of(tarifa));
        when(historialRepo.findByVueloIdAndTipoTarifaAndFechaCapturaGreaterThanEqualOrderByFechaCapturaAsc(
                eq(10), eq("basica"), any(LocalDateTime.class))).thenReturn(historial);

        VueloDetalleDTO resultado = detalleService.obtenerDetalle(100L);

        assertThat(resultado.recomendacion()).startsWith("Monitorear");
        assertThat(resultado.semaforo()).isEqualTo("amarillo");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 6: Semaforo rojo cuando el precio esta muy por encima del promedio
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe asignar semaforo rojo cuando el precio esta muy por encima del promedio")
    void cuandoPrecioMuyAlto_debeAsignarSemaforoRojo() {
        List<HistorialPrecio> historial = List.of(
                crearHistorial("2026-06-01", 200.0),
                crearHistorial("2026-06-02", 200.0)
        );
        when(tarifaRepo.findById(100)).thenReturn(Optional.of(tarifa));
        when(historialRepo.findByVueloIdAndTipoTarifaAndFechaCapturaGreaterThanEqualOrderByFechaCapturaAsc(
                eq(10), eq("basica"), any(LocalDateTime.class))).thenReturn(historial);

        VueloDetalleDTO resultado = detalleService.obtenerDetalle(100L);

        assertThat(resultado.semaforo()).isEqualTo("rojo");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 7: El detalle mapea correctamente datos del vuelo y la tarifa
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe mapear correctamente los datos del vuelo y la tarifa al DTO")
    void debeMapearCorrectamenteLosDatos() {
        when(tarifaRepo.findById(100)).thenReturn(Optional.of(tarifa));
        when(historialRepo.findByVueloIdAndTipoTarifaAndFechaCapturaGreaterThanEqualOrderByFechaCapturaAsc(
                any(), any(), any())).thenReturn(List.of());

        VueloDetalleDTO resultado = detalleService.obtenerDetalle(100L);

        assertThat(resultado.aerolinea()).isEqualTo("LATAM");
        assertThat(resultado.origen()).isEqualTo("LIM");
        assertThat(resultado.destino()).isEqualTo("CUZ");
        assertThat(resultado.horaSalida()).isEqualTo("08:30");
        assertThat(resultado.horaLlegada()).isEqualTo("09:50");
        assertThat(resultado.duracion()).isEqualTo("1h 20m");
        assertThat(resultado.precioActual()).isEqualTo(250.0);
        assertThat(resultado.incluyeEquipaje()).isFalse();
    }

    private HistorialPrecio crearHistorial(String fecha, double precio) {
        HistorialPrecio h = new HistorialPrecio();
        h.setVuelo(vuelo);
        h.setTipoTarifa("basica");
        h.setPrecio(BigDecimal.valueOf(precio));
        h.setFechaCaptura(LocalDate.parse(fecha).atStartOfDay());
        return h;
    }
}
