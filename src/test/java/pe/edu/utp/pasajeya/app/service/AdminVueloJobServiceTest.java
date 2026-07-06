package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.AdminJobEstadoDTO;
import pe.edu.utp.pasajeya.app.repository.HistorialPrecioRepository;
import pe.edu.utp.pasajeya.app.repository.TarifaRepository;
import pe.edu.utp.pasajeya.app.repository.VueloRepository;
import pe.edu.utp.pasajeya.app.service.impl.AdminVueloJobServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminVueloJobServiceTest {

    @Mock private VueloRepository vueloRepo;
    @Mock private TarifaRepository tarifaRepo;
    @Mock private HistorialPrecioRepository historialRepo;
    @Mock private HistorialPrecioJobService jobService;

    @InjectMocks
    private AdminVueloJobServiceImpl adminVueloJobService;

    @Test
    @DisplayName("obtenerEstado sin ejecuciones previas debe devolver fechas nulas")
    void obtenerEstado_sinEjecucionesPrevias() {
        when(jobService.getUltimaEjecucion()).thenReturn(null);
        when(jobService.getTasaCapturaMs()).thenReturn(21600000L);
        when(tarifaRepo.count()).thenReturn(0L);
        when(vueloRepo.count()).thenReturn(0L);
        when(historialRepo.count()).thenReturn(0L);

        AdminJobEstadoDTO estado = adminVueloJobService.obtenerEstado();

        assertThat(estado.ultimaEjecucion()).isNull();
        assertThat(estado.proximaEjecucionEstimada()).isNull();
        assertThat(estado.tasaCapturaMs()).isEqualTo(21600000L);
    }

    @Test
    @DisplayName("obtenerEstado con ejecución previa calcula la próxima ejecución estimada")
    void obtenerEstado_conEjecucionPrevia() {
        LocalDateTime ahora = LocalDateTime.now();
        when(jobService.getUltimaEjecucion()).thenReturn(ahora);
        when(jobService.getTasaCapturaMs()).thenReturn(21600000L);
        when(tarifaRepo.count()).thenReturn(120L);
        when(vueloRepo.count()).thenReturn(50L);
        when(historialRepo.count()).thenReturn(300L);

        AdminJobEstadoDTO estado = adminVueloJobService.obtenerEstado();

        assertThat(estado.ultimaEjecucion()).isNotNull();
        assertThat(estado.proximaEjecucionEstimada()).isNotNull();
        assertThat(estado.totalTarifas()).isEqualTo(120L);
        assertThat(estado.totalVuelos()).isEqualTo(50L);
        assertThat(estado.totalHistorial()).isEqualTo(300L);
    }

    @Test
    @DisplayName("ejecutarAhora delega en el job real de captura de precios")
    void ejecutarAhora_delegaEnJobReal() {
        adminVueloJobService.ejecutarAhora();

        verify(jobService, times(1)).ejecutarAhora();
    }
}
