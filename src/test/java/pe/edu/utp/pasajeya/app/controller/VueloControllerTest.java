package pe.edu.utp.pasajeya.app.controller;

import pe.edu.utp.pasajeya.app.dto.VueloDTO;
import pe.edu.utp.pasajeya.app.dto.VueloDetalleDTO;
import pe.edu.utp.pasajeya.app.security.JwtFilter;
import pe.edu.utp.pasajeya.app.security.JwtUtil;
import pe.edu.utp.pasajeya.app.service.VueloDetalleService;
import pe.edu.utp.pasajeya.app.service.VueloExcelService;
import pe.edu.utp.pasajeya.app.service.VueloService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// /api/vuelos/** es publico (permitAll en SecurityConfig). addFilters=false
// desactiva la cadena de filtros de seguridad real dentro de MockMvc.
@WebMvcTest(VueloController.class)
@AutoConfigureMockMvc(addFilters = false)
class VueloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VueloService vueloService;
    @MockBean
    private VueloExcelService excelService;
    @MockBean
    private VueloDetalleService detalleService;
    @MockBean
    private JwtFilter jwtFilter;
    @MockBean
    private JwtUtil jwtUtil;

    private VueloDTO vueloDePrueba;

    @BeforeEach
    void setUp() {
        vueloDePrueba = new VueloDTO(
                100L, "LATAM", "LIM", "CUZ", "2026-07-01",
                "08:30", "09:50", "1h 20m", 250.0, "basica",
                false, 0, 8, false, false, "verde", "https://latam.com"
        );
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 1: GET /api/vuelos retorna lista de vuelos
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("GET /api/vuelos debe retornar 200 con la lista de vuelos encontrados")
    void getVuelos_debeRetornar200ConLista() throws Exception {
        when(vueloService.buscarVuelos("LIM", "CUZ", "2026-07-01", 1))
                .thenReturn(List.of(vueloDePrueba));

        mockMvc.perform(get("/api/vuelos")
                        .param("origen", "LIM")
                        .param("destino", "CUZ")
                        .param("fecha", "2026-07-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].aerolinea").value("LATAM"));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 2: GET /api/vuelos con pasajeros fuera de rango (>4) debe fallar
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("GET /api/vuelos con pasajeros mayor a 4 debe retornar 400")
    void getVuelos_conPasajerosFueraDeRango_debeRetornar400() throws Exception {
        mockMvc.perform(get("/api/vuelos")
                        .param("origen", "LIM")
                        .param("destino", "CUZ")
                        .param("fecha", "2026-07-01")
                        .param("pasajeros", "5"))
                .andExpect(status().isBadRequest());
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 3: GET /api/vuelos/tarifas/{id} retorna el detalle
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("GET /api/vuelos/tarifas/{id} debe retornar 200 con el detalle de la tarifa")
    void getDetalle_debeRetornar200ConDetalle() throws Exception {
        VueloDetalleDTO detalle = new VueloDetalleDTO(
                100L, 10, "LATAM", "LIM", "CUZ", "2026-07-01",
                "08:30", "09:50", "1h 20m", 250.0, "basica",
                false, 0, 8, false, false, "verde", "https://latam.com",
                List.of(), List.of(), "Monitorear: el precio esta cerca del promedio historico."
        );
        when(detalleService.obtenerDetalle(100L)).thenReturn(detalle);

        mockMvc.perform(get("/api/vuelos/tarifas/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aerolinea").value("LATAM"))
                .andExpect(jsonPath("$.recomendacion").exists());
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 4: GET /api/vuelos/exportar retorna el archivo Excel
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("GET /api/vuelos/exportar debe retornar 200 con el archivo Excel")
    void getExportar_debeRetornar200ConExcel() throws Exception {
        when(vueloService.buscarVuelos("LIM", "CUZ", "2026-07-01", 1))
                .thenReturn(List.of(vueloDePrueba));
        when(excelService.generarExcel(anyList())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/vuelos/exportar")
                        .param("origen", "LIM")
                        .param("destino", "CUZ")
                        .param("fecha", "2026-07-01"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"));
    }
}
