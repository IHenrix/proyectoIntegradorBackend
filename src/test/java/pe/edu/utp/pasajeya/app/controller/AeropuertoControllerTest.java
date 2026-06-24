package pe.edu.utp.pasajeya.app.controller;

import pe.edu.utp.pasajeya.app.dto.AeropuertoDTO;
import pe.edu.utp.pasajeya.app.security.JwtFilter;
import pe.edu.utp.pasajeya.app.security.JwtUtil;
import pe.edu.utp.pasajeya.app.service.AeropuertoService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// /api/aeropuertos es publico (permitAll en SecurityConfig). addFilters=false
// desactiva la cadena de filtros de seguridad real dentro de MockMvc (no se
// necesita @WithMockUser porque el endpoint no exige autenticacion). Aun asi
// JwtFilter/JwtUtil se mockean porque SecurityConfig los necesita como beans
// para poder construirse al arrancar el contexto de @WebMvcTest.
@WebMvcTest(AeropuertoController.class)
@AutoConfigureMockMvc(addFilters = false)
class AeropuertoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AeropuertoService aeropuertoService;

    @MockBean
    private JwtFilter jwtFilter;
    @MockBean
    private JwtUtil jwtUtil;

    // ═══════════════════════════════════════════════════
    // PRUEBA 1: GET /api/aeropuertos devuelve lista JSON
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("GET /api/aeropuertos debe retornar 200 con la lista de aeropuertos")
    void getAeropuertos_debeRetornar200ConLista() throws Exception {
        AeropuertoDTO lima = new AeropuertoDTO("LIM", "Lima", "Jorge Chavez", "Perú");
        when(aeropuertoService.listar()).thenReturn(List.of(lima));

        mockMvc.perform(get("/api/aeropuertos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].code").value("LIM"))
                .andExpect(jsonPath("$[0].ciudad").value("Lima"));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 2: GET /api/aeropuertos con lista vacia
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("GET /api/aeropuertos debe retornar 200 con lista vacia si no hay aeropuertos")
    void getAeropuertos_sinDatos_debeRetornarListaVacia() throws Exception {
        when(aeropuertoService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/api/aeropuertos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(0));
    }
}
