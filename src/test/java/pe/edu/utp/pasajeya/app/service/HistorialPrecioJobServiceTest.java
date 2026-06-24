package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.model.Aerolinea;
import pe.edu.utp.pasajeya.app.model.Tarifa;
import pe.edu.utp.pasajeya.app.model.Vuelo;
import pe.edu.utp.pasajeya.app.repository.HistorialPrecioRepository;
import pe.edu.utp.pasajeya.app.repository.TarifaRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistorialPrecioJobServiceTest {

    @Mock
    private TarifaRepository tarifaRepo;
    @Mock
    private HistorialPrecioRepository historialRepo;
    @Mock
    private AlertaService alertaService;

    @InjectMocks
    private HistorialPrecioJobService jobService;

    // ═══════════════════════════════════════════════════
    // PRUEBA 1: Captura un registro de historial por cada tarifa existente
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe guardar un registro de historial por cada tarifa existente")
    void cuandoHayTarifas_debeGuardarUnHistorialPorCadaUna() {
        Aerolinea aerolinea = new Aerolinea();
        aerolinea.setNombre("LATAM");
        Vuelo vuelo = new Vuelo();
        vuelo.setId(1);
        vuelo.setAerolinea(aerolinea);
        vuelo.setOrigen("LIM");
        vuelo.setDestino("CUZ");
        vuelo.setFechaSalida(LocalDate.of(2026, 7, 1));
        vuelo.setHoraSalida(LocalTime.of(8, 0));
        vuelo.setDuracionMin(80);

        Tarifa tarifa1 = new Tarifa();
        tarifa1.setVuelo(vuelo);
        tarifa1.setTipo("basica");
        tarifa1.setPrecio(BigDecimal.valueOf(200.0));

        Tarifa tarifa2 = new Tarifa();
        tarifa2.setVuelo(vuelo);
        tarifa2.setTipo("flex");
        tarifa2.setPrecio(BigDecimal.valueOf(350.0));

        when(tarifaRepo.findAll()).thenReturn(List.of(tarifa1, tarifa2));

        jobService.capturarPrecios();

        verify(historialRepo, times(2)).save(any());
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 2: Siempre dispara la evaluacion de alertas activas al finalizar
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe evaluar las alertas activas despues de capturar precios")
    void cuandoCapturaPrecios_debeEvaluarAlertasActivas() {
        when(tarifaRepo.findAll()).thenReturn(List.of());

        jobService.capturarPrecios();

        verify(alertaService, times(1)).evaluarAlertasActivas();
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 3: Sin tarifas no debe guardar ningun historial pero igual evalua alertas
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Sin tarifas no debe guardar historial pero si debe evaluar alertas")
    void sinTarifas_noDebeGuardarHistorialPeroSiEvaluarAlertas() {
        when(tarifaRepo.findAll()).thenReturn(List.of());

        jobService.capturarPrecios();

        verify(historialRepo, never()).save(any());
        verify(alertaService, times(1)).evaluarAlertasActivas();
    }
}
