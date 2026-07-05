package pe.edu.utp.pasajeya.app.controller;

import pe.edu.utp.pasajeya.app.dto.ActualizarPerfilRequestDTO;
import pe.edu.utp.pasajeya.app.model.*;
import pe.edu.utp.pasajeya.app.repository.PagoRepository;
import pe.edu.utp.pasajeya.app.repository.PersonaRepository;
import pe.edu.utp.pasajeya.app.repository.SuscripcionRepository;
import pe.edu.utp.pasajeya.app.repository.TipoDocumentoRepository;
import pe.edu.utp.pasajeya.app.repository.UsuarioRepository;
import pe.edu.utp.pasajeya.app.security.JwtFilter;
import pe.edu.utp.pasajeya.app.security.JwtUtil;
import pe.edu.utp.pasajeya.app.service.SuscripcionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Solo carga la capa web (Controller). PerfilController resuelve el usuario
// leyendo el header Authorization directamente con JwtUtil (no usa
// Authentication de Spring Security), por eso no se necesita @WithMockUser
// para el contexto de seguridad — solo mockear jwtUtil.extraerEmail().
@WebMvcTest(PerfilController.class)
class PerfilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioRepository usuarioRepo;
    @MockitoBean
    private PersonaRepository personaRepo;
    @MockitoBean
    private TipoDocumentoRepository tipoDocRepo;
    @MockitoBean
    private SuscripcionRepository suscripcionRepo;
    @MockitoBean
    private PagoRepository pagoRepo;
    @MockitoBean
    private SuscripcionService suscripcionService;
    @MockitoBean
    private JwtUtil jwtUtil;
    @MockitoBean
    private PasswordEncoder passwordEncoder;
    @MockitoBean
    private JwtFilter jwtFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private Usuario usuario;
    private Persona persona;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());

        Rol rol = new Rol();
        rol.setNombre("usuario_free");

        persona = new Persona();
        persona.setId(1);
        persona.setNombre("Ana");
        persona.setApellidoPaterno("Garcia");
        persona.setTelefono("987654321");

        usuario = new Usuario();
        usuario.setId(1);
        usuario.setEmail("ana@test.com");
        usuario.setPasswordHash("hash-actual");
        usuario.setRol(rol);
        usuario.setPersona(persona);

        when(jwtUtil.extraerEmail("token-valido")).thenReturn("ana@test.com");
        when(usuarioRepo.findByEmail("ana@test.com")).thenReturn(Optional.of(usuario));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 1: GET /api/perfil retorna los datos del usuario autenticado
    // ═══════════════════════════════════════════════════
    @Test
    @WithMockUser
    @DisplayName("GET /api/perfil debe retornar 200 con los datos del usuario")
    void getPerfil_debeRetornar200ConDatos() throws Exception {
        mockMvc.perform(get("/api/perfil")
                        .header("Authorization", "Bearer token-valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.email").value("ana@test.com"))
                .andExpect(jsonPath("$.rol").value("usuario_free"));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 2: PUT /api/perfil actualiza nombre y apellido
    // ═══════════════════════════════════════════════════
    @Test
    @WithMockUser
    @DisplayName("PUT /api/perfil debe actualizar nombre y apellido correctamente")
    void putPerfil_debeActualizarNombreYApellido() throws Exception {
        ActualizarPerfilRequestDTO dto = new ActualizarPerfilRequestDTO(
                "luis", "torres", null, null, null, null, null, null, null, null);

        mockMvc.perform(put("/api/perfil")
                        .with(csrf())
                        .header("Authorization", "Bearer token-valido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Luis"))
                .andExpect(jsonPath("$.apellidoPaterno").value("Torres"));

        verify(usuarioRepo, times(1)).save(usuario);
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 3: PUT /api/perfil con password actual incorrecta debe fallar
    // ═══════════════════════════════════════════════════
    @Test
    @WithMockUser
    @DisplayName("PUT /api/perfil con password actual incorrecta debe retornar 400")
    void putPerfil_conPasswordActualIncorrecta_debeRetornar400() throws Exception {
        ActualizarPerfilRequestDTO dto = new ActualizarPerfilRequestDTO(
                null, null, null, null, null, null, null, null, "password-malo", "nuevoPassword123");
        when(passwordEncoder.matches("password-malo", "hash-actual")).thenReturn(false);

        mockMvc.perform(put("/api/perfil")
                        .with(csrf())
                        .header("Authorization", "Bearer token-valido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La contraseña actual es incorrecta"));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 4: PUT /api/perfil con documento duplicado debe fallar
    // ═══════════════════════════════════════════════════
    @Test
    @WithMockUser
    @DisplayName("PUT /api/perfil con documento duplicado debe retornar 400")
    void putPerfil_conDocumentoDuplicado_debeRetornar400() throws Exception {
        ActualizarPerfilRequestDTO dto = new ActualizarPerfilRequestDTO(
                null, null, null, null, null, null, "DNI", "12345678", null, null);
        when(personaRepo.existsByNroDocumentoAndIdNot("12345678", 1)).thenReturn(true);

        mockMvc.perform(put("/api/perfil")
                        .with(csrf())
                        .header("Authorization", "Bearer token-valido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El número de documento ya está registrado por otro usuario"));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 5: GET /api/perfil/suscripcion sin suscripcion vigente retorna 204
    // ═══════════════════════════════════════════════════
    @Test
    @WithMockUser
    @DisplayName("GET /api/perfil/suscripcion debe retornar 204 cuando no hay suscripcion vigente")
    void getSuscripcionActiva_sinVigente_debeRetornar204() throws Exception {
        when(suscripcionRepo.expirarVencidas(any(LocalDate.class))).thenReturn(0);
        when(suscripcionRepo.findVigente(eq(1), any(LocalDate.class))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/perfil/suscripcion")
                        .header("Authorization", "Bearer token-valido"))
                .andExpect(status().isNoContent());
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 6: GET /api/perfil/suscripcion con suscripcion vigente retorna 200
    // ═══════════════════════════════════════════════════
    @Test
    @WithMockUser
    @DisplayName("GET /api/perfil/suscripcion debe retornar 200 con los datos cuando hay suscripcion vigente")
    void getSuscripcionActiva_conVigente_debeRetornar200() throws Exception {
        Plan planPremium = new Plan();
        planPremium.setNombre("Premium Mensual");
        planPremium.setDuracionDias(30);

        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setId(5);
        suscripcion.setPlan(planPremium);
        suscripcion.setIdPagoOrigen(10);
        suscripcion.setPrecioPagado(BigDecimal.valueOf(15.0));
        suscripcion.setFechaInicio(LocalDate.of(2026, 6, 1));
        suscripcion.setFechaFin(LocalDate.of(2026, 7, 1));
        suscripcion.setEstado("activa");
        suscripcion.setMetodoPago("yape");

        Pago pago = new Pago();
        pago.setRefInterna("748291");

        when(suscripcionRepo.expirarVencidas(any(LocalDate.class))).thenReturn(0);
        when(suscripcionRepo.findVigente(eq(1), any(LocalDate.class))).thenReturn(Optional.of(suscripcion));
        when(pagoRepo.findById(10)).thenReturn(Optional.of(pago));

        mockMvc.perform(get("/api/perfil/suscripcion")
                        .header("Authorization", "Bearer token-valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planNombre").value("Premium Mensual"))
                .andExpect(jsonPath("$.tipoPlan").value("mensual"))
                .andExpect(jsonPath("$.refInterna").value("748291"));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 7: GET /api/perfil/suscripciones retorna el historial completo
    // ═══════════════════════════════════════════════════
    @Test
    @WithMockUser
    @DisplayName("GET /api/perfil/suscripciones debe retornar el historial de suscripciones")
    void getHistorialSuscripciones_debeRetornarLista() throws Exception {
        Plan planAnual = new Plan();
        planAnual.setNombre("Premium Anual");
        planAnual.setDuracionDias(365);

        Suscripcion suscripcionVencida = new Suscripcion();
        suscripcionVencida.setId(3);
        suscripcionVencida.setPlan(planAnual);
        suscripcionVencida.setIdPagoOrigen(8);
        suscripcionVencida.setPrecioPagado(BigDecimal.valueOf(120.0));
        suscripcionVencida.setFechaInicio(LocalDate.of(2025, 1, 1));
        suscripcionVencida.setFechaFin(LocalDate.of(2026, 1, 1));
        suscripcionVencida.setEstado("vencida");
        suscripcionVencida.setMetodoPago("tarjeta");

        when(suscripcionRepo.expirarVencidas(any(LocalDate.class))).thenReturn(0);
        when(suscripcionRepo.findHistorial(1)).thenReturn(List.of(suscripcionVencida));
        when(pagoRepo.findById(8)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/perfil/suscripciones")
                        .header("Authorization", "Bearer token-valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].tipoPlan").value("anual"))
                .andExpect(jsonPath("$[0].refInterna").value("—"));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 8: GET /api/perfil con usuario no encontrado debe retornar 400
    // ═══════════════════════════════════════════════════
    @Test
    @WithMockUser
    @DisplayName("GET /api/perfil con usuario inexistente debe retornar 400")
    void getPerfil_conUsuarioInexistente_debeRetornar400() throws Exception {
        when(jwtUtil.extraerEmail("token-otro")).thenReturn("inexistente@test.com");
        when(usuarioRepo.findByEmail("inexistente@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/perfil")
                        .header("Authorization", "Bearer token-otro"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Usuario no encontrado"));
    }
}
