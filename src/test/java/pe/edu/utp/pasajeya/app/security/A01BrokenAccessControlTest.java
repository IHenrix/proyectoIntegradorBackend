package pe.edu.utp.pasajeya.app.security;

import pe.edu.utp.pasajeya.app.controller.AlertaController;
import pe.edu.utp.pasajeya.app.service.AlertaExcelService;
import pe.edu.utp.pasajeya.app.service.AlertaPdfService;
import pe.edu.utp.pasajeya.app.service.AlertaService;
import pe.edu.utp.pasajeya.app.repository.UsuarioRepository;
import pe.edu.utp.pasajeya.app.model.Rol;
import pe.edu.utp.pasajeya.app.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import jakarta.servlet.FilterChain;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PRUEBAS DE SEGURIDAD — A01: Control de Acceso Roto
 *
 * Vulnerabilidad del taller (Sistema de Matrícula — CÓDIGO VULNERABLE):
 *   @GetMapping("/historial/{estudianteId}")
 *   // No verifica que estudianteId pertenezca al usuario en sesión
 *   // Cualquier usuario autenticado puede acceder a /admin/reportes
 *
 * Corrección en PasajeYa:
 *   - Todos los endpoints privados requieren JWT válido (SecurityConfig: anyRequest().authenticated()).
 *   - AlertaController recibe Authentication auth y pasa auth.getName() (email del token)
 *     directamente al service — nunca acepta un ID arbitrario de la URL.
 *   - El reporte Excel/PDF verifica el rol (verificarPremium) antes de servir el recurso.
 *   - Endpoints sin token reciben 401 Unauthorized automáticamente.
 */
@WebMvcTest(AlertaController.class)
@DisplayName("A01 — Control de Acceso Roto: PasajeYa exige JWT y verifica roles")
class A01BrokenAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private AlertaService alertaService;
    @MockitoBean private AlertaExcelService excelService;
    @MockitoBean private AlertaPdfService pdfService;
    @MockitoBean private UsuarioRepository usuarioRepo;
    @MockitoBean private JwtFilter jwtFilter;
    @MockitoBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 1: Petición sin token recibe 401 Unauthorized
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("GET /api/alertas sin token JWT debe retornar 401 Unauthorized")
    void sinToken_debeRetornar401() throws Exception {
        mockMvc.perform(get("/api/alertas"))
                .andExpect(status().isUnauthorized());
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 2: Usuario autenticado solo accede a SUS propias alertas (no a las de otro)
    // ═══════════════════════════════════════════════════
    @Test
    @WithMockUser(username = "ana@test.com")
    @DisplayName("GET /api/alertas solo devuelve las alertas del usuario autenticado en el token")
    void alertas_soloDelUsuarioAutenticado() throws Exception {
        when(alertaService.listar("ana@test.com")).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/alertas"))
                .andExpect(status().isOk());

        verify(alertaService, times(1)).listar("ana@test.com");
        verify(alertaService, never()).listar("otro@test.com");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 3: Usuario free no puede acceder al reporte Excel (requiere premium)
    // ═══════════════════════════════════════════════════
    @Test
    @WithMockUser(username = "free@test.com")
    @DisplayName("GET /api/alertas/reporte/excel con usuario free debe retornar 400 (REQUIERE_PREMIUM)")
    void usuarioFree_noDebeAccederAlReporteExcel() throws Exception {
        Rol rolFree = new Rol();
        rolFree.setNombre("usuario_free");

        Usuario usuarioFree = new Usuario();
        usuarioFree.setRol(rolFree);

        when(usuarioRepo.findByEmail("free@test.com")).thenReturn(Optional.of(usuarioFree));

        mockMvc.perform(get("/api/alertas/reporte/excel"))
                .andExpect(status().isBadRequest());
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 4: Usuario premium SÍ puede acceder al reporte Excel
    // ═══════════════════════════════════════════════════
    @Test
    @WithMockUser(username = "premium@test.com")
    @DisplayName("GET /api/alertas/reporte/excel con usuario premium debe retornar 200")
    void usuarioPremium_debeAccederAlReporteExcel() throws Exception {
        Rol rolPremium = new Rol();
        rolPremium.setNombre("usuario_premium");

        pe.edu.utp.pasajeya.app.model.Persona persona = new pe.edu.utp.pasajeya.app.model.Persona();
        persona.setNombre("Luis");
        persona.setApellidoPaterno("Torres");

        Usuario usuarioPremium = new Usuario();
        usuarioPremium.setRol(rolPremium);
        usuarioPremium.setPersona(persona);

        when(usuarioRepo.findByEmail("premium@test.com")).thenReturn(Optional.of(usuarioPremium));
        when(alertaService.listar("premium@test.com")).thenReturn(java.util.List.of());
        when(excelService.generar(any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/alertas/reporte/excel"))
                .andExpect(status().isOk());
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 5: DELETE /api/alertas/{id} sin token recibe 401
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("DELETE /api/alertas/1 sin token JWT debe ser rechazado (Spring Security devuelve 403)")
    void eliminarSinToken_debeRetornar401() throws Exception {
        mockMvc.perform(delete("/api/alertas/1"))
                .andExpect(status().isForbidden());

        verify(alertaService, never()).eliminar(any(), any());
    }
}
