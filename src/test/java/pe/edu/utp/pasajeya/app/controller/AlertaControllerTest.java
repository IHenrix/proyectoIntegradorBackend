package pe.edu.utp.pasajeya.app.controller;

import pe.edu.utp.pasajeya.app.dto.AlertaDTO;
import pe.edu.utp.pasajeya.app.dto.CrearAlertaRequestDTO;
import pe.edu.utp.pasajeya.app.model.Persona;
import pe.edu.utp.pasajeya.app.model.Rol;
import pe.edu.utp.pasajeya.app.model.Usuario;
import pe.edu.utp.pasajeya.app.repository.UsuarioRepository;
import pe.edu.utp.pasajeya.app.security.JwtFilter;
import pe.edu.utp.pasajeya.app.security.JwtUtil;
import pe.edu.utp.pasajeya.app.service.AlertaExcelService;
import pe.edu.utp.pasajeya.app.service.AlertaPdfService;
import pe.edu.utp.pasajeya.app.service.AlertaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Solo carga la capa web (Controller). SecurityConfig (real) sigue
// arrancando porque esta en el classpath, asi que JwtFilter y JwtUtil se
// mockean para satisfacer sus dependencias sin necesitar un token real.
// JwtFilter se configura para dejar pasar la request sin tocarla (doFilter),
// de modo que el filtro de Spring Security Test (que traduce @WithMockUser
// en una Authentication real dentro del SecurityContext) siga funcionando.
@WebMvcTest(AlertaController.class)
class AlertaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlertaService alertaService;

    @MockBean
    private AlertaExcelService excelService;
    @MockBean
    private AlertaPdfService pdfService;
    @MockBean
    private UsuarioRepository usuarioRepo;
    @MockBean
    private JwtFilter jwtFilter;
    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private AlertaDTO alertaDePrueba;

    @BeforeEach
    void setUp() throws Exception {
        alertaDePrueba = new AlertaDTO(
                1, 100L, 10, "LATAM", "LIM", "CUZ",
                "2026-07-01", "08:30", "basica",
                200.0, 250.0, "987654321", true,
                "2026-06-23T10:00:00", "Activa"
        );

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 1: GET /api/alertas → devuelve lista JSON
    // ═══════════════════════════════════════════════════
    @Test
    @WithMockUser(username = "ana@test.com")
    @DisplayName("GET /api/alertas debe retornar lista de alertas del usuario autenticado")
    void getTodasLasAlertas_debeRetornar200ConLista() throws Exception {
        when(alertaService.listar("ana@test.com")).thenReturn(List.of(alertaDePrueba));

        mockMvc.perform(get("/api/alertas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].origen").value("LIM"))
                .andExpect(jsonPath("$[0].precioObjetivo").value(200.0));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 2: POST /api/alertas → crear nueva alerta
    // ═══════════════════════════════════════════════════
    @Test
    @WithMockUser(username = "ana@test.com")
    @DisplayName("POST /api/alertas debe crear alerta y retornar 200")
    void postAlerta_debeCrearYRetornar200() throws Exception {
        CrearAlertaRequestDTO request = new CrearAlertaRequestDTO(
                100L, null, null, null, null, 200.0, "987654321");
        when(alertaService.crear(eq("ana@test.com"), any(CrearAlertaRequestDTO.class)))
                .thenReturn(alertaDePrueba);

        String jsonRequest = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/alertas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origen").value("LIM"))
                .andExpect(jsonPath("$.precioObjetivo").value(200.0));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 3: PATCH /api/alertas/{id}/pausar → actualizar estado
    // ═══════════════════════════════════════════════════
    @Test
    @WithMockUser(username = "ana@test.com")
    @DisplayName("PATCH /api/alertas/1/pausar debe retornar la alerta pausada")
    void patchPausar_debeRetornar200ConAlertaPausada() throws Exception {
        AlertaDTO pausada = new AlertaDTO(
                1, 100L, 10, "LATAM", "LIM", "CUZ",
                "2026-07-01", "08:30", "basica",
                200.0, 250.0, "987654321", false,
                "2026-06-23T10:00:00", "Pausada"
        );
        when(alertaService.pausar("ana@test.com", 1)).thenReturn(pausada);

        mockMvc.perform(patch("/api/alertas/1/pausar").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(false))
                .andExpect(jsonPath("$.estado").value("Pausada"));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 4: DELETE /api/alertas/{id} → eliminar alerta
    // ═══════════════════════════════════════════════════
    @Test
    @WithMockUser(username = "ana@test.com")
    @DisplayName("DELETE /api/alertas/1 debe retornar 204 No Content")
    void deleteAlerta_debeRetornar204() throws Exception {
        doNothing().when(alertaService).eliminar("ana@test.com", 1);

        mockMvc.perform(delete("/api/alertas/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(alertaService, times(1)).eliminar("ana@test.com", 1);
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 5: GET /api/alertas/reporte/excel sin ser premium → error
    // ═══════════════════════════════════════════════════
    @Test
    @WithMockUser(username = "ana@test.com")
    @DisplayName("GET /api/alertas/reporte/excel debe fallar si el usuario no es premium")
    void getReporteExcel_usuarioNoPremium_debeFallar() throws Exception {
        Rol rolFree = new Rol();
        rolFree.setNombre("usuario_free");
        Usuario usuarioFree = new Usuario();
        usuarioFree.setEmail("ana@test.com");
        usuarioFree.setRol(rolFree);
        when(usuarioRepo.findByEmail("ana@test.com")).thenReturn(Optional.of(usuarioFree));

        mockMvc.perform(get("/api/alertas/reporte/excel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("REQUIERE_PREMIUM"));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 6: GET /api/alertas/reporte/excel siendo premium → 200
    // ═══════════════════════════════════════════════════
    @Test
    @WithMockUser(username = "premium@test.com")
    @DisplayName("GET /api/alertas/reporte/excel debe retornar 200 si el usuario es premium")
    void getReporteExcel_usuarioPremium_debeRetornar200() throws Exception {
        Rol rolPremium = new Rol();
        rolPremium.setNombre("usuario_premium");
        Persona persona = new Persona();
        persona.setNombre("Luis");
        persona.setApellidoPaterno("Torres");
        Usuario usuarioPremium = new Usuario();
        usuarioPremium.setEmail("premium@test.com");
        usuarioPremium.setRol(rolPremium);
        usuarioPremium.setPersona(persona);
        when(usuarioRepo.findByEmail("premium@test.com")).thenReturn(Optional.of(usuarioPremium));
        when(alertaService.listar("premium@test.com")).thenReturn(List.of(alertaDePrueba));
        when(excelService.generar(anyList(), anyString())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/alertas/reporte/excel"))
                .andExpect(status().isOk());
    }
}
