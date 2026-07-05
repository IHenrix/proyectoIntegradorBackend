package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.AdminHistorialPrecioDTO;
import pe.edu.utp.pasajeya.app.model.Aerolinea;
import pe.edu.utp.pasajeya.app.model.HistorialPrecio;
import pe.edu.utp.pasajeya.app.model.Vuelo;
import pe.edu.utp.pasajeya.app.repository.HistorialPrecioRepository;
import pe.edu.utp.pasajeya.app.service.impl.AdminHistorialPrecioServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminHistorialPrecioServiceTest {

    @Mock private HistorialPrecioRepository historialRepo;

    @InjectMocks
    private AdminHistorialPrecioServiceImpl historialService;

    @Test
    @DisplayName("buscar() mapea HistorialPrecio a DTO incluyendo datos del vuelo y aerolínea")
    void buscar_mapeaCorrectamente() {
        Aerolinea latam = new Aerolinea();
        latam.setNombre("LATAM Airlines Peru");

        Vuelo vuelo = new Vuelo();
        vuelo.setId(5);
        vuelo.setAerolinea(latam);
        vuelo.setOrigen("LIM");
        vuelo.setDestino("CUZ");

        HistorialPrecio hp = new HistorialPrecio();
        hp.setVuelo(vuelo);
        hp.setPrecio(new BigDecimal("199.90"));
        hp.setTipoTarifa("basica");
        hp.setFechaCaptura(LocalDateTime.of(2026, 6, 1, 10, 0));

        when(historialRepo.buscarConFiltros(5, "LIM", "CUZ", null, null, Limit.of(1000)))
                .thenReturn(List.of(hp));

        List<AdminHistorialPrecioDTO> resultado = historialService.buscar(5, "LIM", "CUZ", null, null);

        assertThat(resultado).hasSize(1);
        AdminHistorialPrecioDTO dto = resultado.get(0);
        assertThat(dto.idVuelo()).isEqualTo(5);
        assertThat(dto.aerolinea()).isEqualTo("LATAM Airlines Peru");
        assertThat(dto.origen()).isEqualTo("LIM");
        assertThat(dto.destino()).isEqualTo("CUZ");
        assertThat(dto.precio()).isEqualByComparingTo("199.90");
        assertThat(dto.tipoTarifa()).isEqualTo("basica");
    }

    @Test
    @DisplayName("buscar() sin filtros pasa null a la query y devuelve todo lo que retorne el repo")
    void buscar_sinFiltros_pasaNullsAlRepo() {
        when(historialRepo.buscarConFiltros(null, null, null, null, null, Limit.of(1000))).thenReturn(List.of());

        List<AdminHistorialPrecioDTO> resultado = historialService.buscar(null, null, null, null, null);

        assertThat(resultado).isEmpty();
        verify(historialRepo).buscarConFiltros(null, null, null, null, null, Limit.of(1000));
    }

    @Test
    @DisplayName("buscar() con rango de fechas lo reenvía tal cual al repositorio")
    void buscar_conRangoFechas_reenviaAlRepo() {
        LocalDateTime desde = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 1, 31, 23, 59, 59);
        when(historialRepo.buscarConFiltros(any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        historialService.buscar(null, null, null, desde, hasta);

        verify(historialRepo).buscarConFiltros(null, null, null, desde, hasta, Limit.of(1000));
    }

    @Test
    @DisplayName("buscar() aplica un tope de filas para no colgar con tablas de millones de registros")
    void buscar_aplicaLimiteDeFilas() {
        when(historialRepo.buscarConFiltros(any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        historialService.buscar(null, "LIM", "CUZ", null, null);

        verify(historialRepo).buscarConFiltros(null, "LIM", "CUZ", null, null, Limit.of(1000));
    }
}
