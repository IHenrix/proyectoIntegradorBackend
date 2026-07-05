package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.AdminDashboardDTO;
import pe.edu.utp.pasajeya.app.repository.AlertaRepository;
import pe.edu.utp.pasajeya.app.repository.PagoRepository;
import pe.edu.utp.pasajeya.app.repository.SuscripcionRepository;
import pe.edu.utp.pasajeya.app.repository.UsuarioRepository;
import pe.edu.utp.pasajeya.app.service.impl.AdminDashboardServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock private UsuarioRepository usuarioRepo;
    @Mock private AlertaRepository alertaRepo;
    @Mock private SuscripcionRepository suscripcionRepo;
    @Mock private PagoRepository pagoRepo;

    @InjectMocks
    private AdminDashboardServiceImpl dashboardService;

    @Test
    @DisplayName("obtenerMetricas() arma el DTO agregando todos los repos correctamente")
    void obtenerMetricas_armaDtoCompleto() {
        when(usuarioRepo.contarPorRol()).thenReturn(List.of(
                new Object[]{"usuario_free", 5L},
                new Object[]{"usuario_premium", 2L},
                new Object[]{"admin", 1L}
        ));
        when(usuarioRepo.countByActivoTrue()).thenReturn(7L);
        when(usuarioRepo.count()).thenReturn(8L);
        when(pagoRepo.sumarIngresosTotales()).thenReturn(Optional.of(new BigDecimal("350.00")));
        when(alertaRepo.countByActivaTrue()).thenReturn(4L);
        when(suscripcionRepo.countByEstado("activa")).thenReturn(2L);
        when(suscripcionRepo.countByEstado("vencida")).thenReturn(1L);
        when(suscripcionRepo.countByEstado("cancelada")).thenReturn(3L);

        AdminDashboardDTO dto = dashboardService.obtenerMetricas();

        assertThat(dto.usuariosPorRol()).containsEntry("usuario_free", 5L)
                .containsEntry("usuario_premium", 2L)
                .containsEntry("admin", 1L);
        assertThat(dto.usuariosActivos()).isEqualTo(7L);
        assertThat(dto.usuariosInactivos()).isEqualTo(1L); // 8 total - 7 activos
        assertThat(dto.ingresosTotales()).isEqualByComparingTo("350.00");
        assertThat(dto.alertasActivas()).isEqualTo(4L);
        assertThat(dto.suscripcionesActivas()).isEqualTo(2L);
        assertThat(dto.suscripcionesVencidas()).isEqualTo(1L);
        assertThat(dto.suscripcionesCanceladas()).isEqualTo(3L);
    }

    @Test
    @DisplayName("obtenerMetricas() sin pagos aprobados devuelve ingresos en cero, no null")
    void obtenerMetricas_sinPagos_ingresosEnCero() {
        when(usuarioRepo.contarPorRol()).thenReturn(List.of());
        when(usuarioRepo.countByActivoTrue()).thenReturn(0L);
        when(usuarioRepo.count()).thenReturn(0L);
        when(pagoRepo.sumarIngresosTotales()).thenReturn(Optional.empty());
        when(alertaRepo.countByActivaTrue()).thenReturn(0L);
        when(suscripcionRepo.countByEstado(org.mockito.ArgumentMatchers.anyString())).thenReturn(0L);

        AdminDashboardDTO dto = dashboardService.obtenerMetricas();

        assertThat(dto.ingresosTotales()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
