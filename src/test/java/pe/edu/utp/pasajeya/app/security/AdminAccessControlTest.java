package pe.edu.utp.pasajeya.app.security;

import pe.edu.utp.pasajeya.app.controller.AdminController;
import pe.edu.utp.pasajeya.app.dto.AdminDashboardDTO;
import pe.edu.utp.pasajeya.app.service.AdminDashboardService;
import pe.edu.utp.pasajeya.app.service.AdminHistorialPrecioExcelService;
import pe.edu.utp.pasajeya.app.service.AdminHistorialPrecioService;
import pe.edu.utp.pasajeya.app.service.AdminSuscripcionService;
import pe.edu.utp.pasajeya.app.service.AdminUsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que /api/admin/** exige el rol ADMIN de verdad (vía @PreAuthorize
 * + GrantedAuthority reales del JWT firmado), no una simple bandera de
 * localStorage editable por el cliente — es la prueba central de que el
 * panel de administración no es accesible por cualquier usuario autenticado.
 */
@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@DisplayName("Panel Admin — exige rol ADMIN real vía @PreAuthorize, no localStorage")
class AdminAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private AdminDashboardService dashboardService;
    @MockitoBean private AdminUsuarioService usuarioService;
    @MockitoBean private AdminHistorialPrecioService historialService;
    @MockitoBean private AdminHistorialPrecioExcelService historialExcelService;
    @MockitoBean private AdminSuscripcionService suscripcionService;
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

    @Test
    @DisplayName("GET /api/admin/dashboard sin autenticación debe retornar 401/403")
    void sinAutenticacion_debeRechazar() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "free@test.com", authorities = {"ROLE_USUARIO_FREE"})
    @DisplayName("GET /api/admin/dashboard con rol usuario_free debe retornar 403 Forbidden")
    void usuarioFree_debeRecibir403() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "premium@test.com", authorities = {"ROLE_USUARIO_PREMIUM"})
    @DisplayName("GET /api/admin/dashboard con rol usuario_premium (sin ser admin) debe retornar 403 Forbidden")
    void usuarioPremium_debeRecibir403() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@pasajeya.com.pe", authorities = {"ROLE_ADMIN"})
    @DisplayName("GET /api/admin/dashboard con rol admin debe retornar 200 con las métricas")
    void admin_debeAcceder200() throws Exception {
        AdminDashboardDTO dto = new AdminDashboardDTO(
                Map.of("usuario_free", 5L, "usuario_premium", 2L, "admin", 1L),
                7L, 1L, new BigDecimal("350.00"), 4L, 2L, 1L, 3L
        );
        when(dashboardService.obtenerMetricas()).thenReturn(dto);

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@pasajeya.com.pe", authorities = {"ROLE_ADMIN"})
    @DisplayName("GET /api/admin/usuarios con rol admin debe retornar 200")
    void admin_accedeAUsuarios200() throws Exception {
        when(usuarioService.listar()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "free@test.com", authorities = {"ROLE_USUARIO_FREE"})
    @DisplayName("GET /api/admin/usuarios con rol free debe retornar 403 (protección a nivel de clase)")
    void free_noAccedeAUsuarios() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "free@test.com", authorities = {"ROLE_USUARIO_FREE"})
    @DisplayName("GET /api/admin/suscripciones con rol free debe retornar 403")
    void free_noAccedeASuscripciones() throws Exception {
        mockMvc.perform(get("/api/admin/suscripciones"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "free@test.com", authorities = {"ROLE_USUARIO_FREE"})
    @DisplayName("GET /api/admin/historial-precios con rol free debe retornar 403")
    void free_noAccedeAHistorialPrecios() throws Exception {
        mockMvc.perform(get("/api/admin/historial-precios"))
                .andExpect(status().isForbidden());
    }
}
