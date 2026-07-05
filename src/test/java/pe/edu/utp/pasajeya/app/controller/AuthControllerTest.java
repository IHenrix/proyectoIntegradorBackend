package pe.edu.utp.pasajeya.app.controller;

import pe.edu.utp.pasajeya.app.dto.LoginRequestDTO;
import pe.edu.utp.pasajeya.app.dto.LoginResponseDTO;
import pe.edu.utp.pasajeya.app.dto.RegistroRequestDTO;
import pe.edu.utp.pasajeya.app.security.JwtFilter;
import pe.edu.utp.pasajeya.app.security.JwtUtil;
import pe.edu.utp.pasajeya.app.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// /api/auth/** es publico (permitAll en SecurityConfig). addFilters=false
// desactiva la cadena de filtros de seguridad real dentro de MockMvc.
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "app.frontend.url=http://localhost:4200")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private JwtFilter jwtFilter;
    @MockitoBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    // ═══════════════════════════════════════════════════
    // PRUEBA 1: POST /api/auth/registro exitoso
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("POST /api/auth/registro debe retornar 200 cuando el registro es exitoso")
    void postRegistro_debeRetornar200() throws Exception {
        RegistroRequestDTO request = new RegistroRequestDTO(
                "Ana", "Garcia", null, "F",
                "ana@test.com", "password123", "987654321",
                null, null, null, "captcha-token");
        doNothing().when(authService).registro(any(RegistroRequestDTO.class));

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authService, times(1)).registro(any(RegistroRequestDTO.class));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 2: POST /api/auth/registro con email invalido debe fallar la validacion
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("POST /api/auth/registro con email invalido debe retornar 400")
    void postRegistro_conEmailInvalido_debeRetornar400() throws Exception {
        RegistroRequestDTO requestInvalido = new RegistroRequestDTO(
                "Ana", "Garcia", null, "F",
                "no-es-un-email", "password123", "987654321",
                null, null, null, "captcha-token");

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).registro(any());
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 3: POST /api/auth/login exitoso
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("POST /api/auth/login debe retornar 200 con el token cuando las credenciales son validas")
    void postLogin_debeRetornar200ConToken() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("ana@test.com", "password123", "captcha-token");
        LoginResponseDTO response = new LoginResponseDTO("jwt-token", "Ana", "usuario_free");
        when(authService.login(any(LoginRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.nombre").value("Ana"));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 4: GET /api/auth/verificar con token valido redirige a /auth?verificado=ok
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("GET /api/auth/verificar con token valido debe redirigir con verificado=ok")
    void getVerificar_conTokenValido_debeRedirigirOk() throws Exception {
        doNothing().when(authService).verificarEmail("token-valido");

        mockMvc.perform(get("/api/auth/verificar").param("token", "token-valido"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost:4200/auth?verificado=ok"));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 5: GET /api/auth/verificar con token invalido redirige a error
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("GET /api/auth/verificar con token invalido debe redirigir con verificado=error")
    void getVerificar_conTokenInvalido_debeRedirigirError() throws Exception {
        doThrow(new RuntimeException("Token inválido")).when(authService).verificarEmail("token-invalido");

        mockMvc.perform(get("/api/auth/verificar").param("token", "token-invalido"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("verificado=error")));
    }
}
