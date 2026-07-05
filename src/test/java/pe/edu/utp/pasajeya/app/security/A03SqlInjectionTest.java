package pe.edu.utp.pasajeya.app.security;

import pe.edu.utp.pasajeya.app.dto.LoginRequestDTO;
import pe.edu.utp.pasajeya.app.dto.LoginResponseDTO;
import pe.edu.utp.pasajeya.app.model.Rol;
import pe.edu.utp.pasajeya.app.model.Usuario;
import pe.edu.utp.pasajeya.app.repository.UsuarioRepository;
import pe.edu.utp.pasajeya.app.service.RecaptchaService;
import pe.edu.utp.pasajeya.app.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * PRUEBAS DE SEGURIDAD — A03: Inyección SQL
 *
 * Vulnerabilidad del taller (Sistema de Matrícula — CÓDIGO VULNERABLE):
 *   String query = "SELECT * FROM usuarios WHERE usuario='" + usuario + "' AND password='" + password + "'";
 *   Con payload "' OR '1'='1" el atacante entra sin contraseña.
 *
 * Corrección en PasajeYa:
 *   - AuthServiceImpl usa Spring Data JPA (usuarioRepo.findByEmail).
 *   - JPA genera PreparedStatement internamente — nunca concatena SQL con input del usuario.
 *   - La contraseña se verifica con BCrypt (passwordEncoder.matches), no con SQL.
 *   - Un payload SQL en el campo email no produce ningún resultado en la BD.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("A03 — Inyección SQL: PasajeYa usa JPA + BCrypt (no SQL concatenado)")
class A03SqlInjectionTest {

    @Mock private UsuarioRepository usuarioRepo;
    @Mock private pe.edu.utp.pasajeya.app.repository.PersonaRepository personaRepo;
    @Mock private pe.edu.utp.pasajeya.app.repository.TokenVerificacionRepository tokenRepo;
    @Mock private pe.edu.utp.pasajeya.app.repository.RolRepository rolRepo;
    @Mock private pe.edu.utp.pasajeya.app.repository.TipoDocumentoRepository tipoDocRepo;
    @Mock private JwtUtil jwtUtil;
    @Mock private pe.edu.utp.pasajeya.app.service.EmailService emailService;
    @Mock private RecaptchaService recaptchaService;

    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "passwordEncoder", passwordEncoder);
        when(recaptchaService.verificar(any())).thenReturn(true);
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 1: Payload SQL clásico en el campo email no entra al sistema
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Payload SQL Injection en email debe ser rechazado — no retorna usuario")
    void payloadSqlEnEmail_debeSerRechazado() {
        String payloadSql = "' OR '1'='1";
        // El servicio normaliza el email (lowerCase + trim), por eso usamos anyString()
        when(usuarioRepo.findByEmail(anyString())).thenReturn(Optional.empty());

        LoginRequestDTO request = new LoginRequestDTO(payloadSql, "cualquier_cosa", "captcha");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Credenciales incorrectas");

        verify(usuarioRepo, times(1)).findByEmail(anyString());
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 2: Payload UNION SELECT en email no produce acceso
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Payload UNION SELECT en email debe ser rechazado")
    void payloadUnionSelectEnEmail_debeSerRechazado() {
        String payloadUnion = "' UNION SELECT 1,2,3--";
        // El servicio normaliza el email (lowerCase + trim), por eso usamos anyString()
        when(usuarioRepo.findByEmail(anyString())).thenReturn(Optional.empty());

        LoginRequestDTO request = new LoginRequestDTO(payloadUnion, "pass", "captcha");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Credenciales incorrectas");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 3: Contraseña correcta con BCrypt permite el acceso (happy path)
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Login legítimo con BCrypt debe funcionar correctamente")
    void loginLegitimo_conBcrypt_debeFuncionar() {
        String hashReal = passwordEncoder.encode("Password123");

        Rol rol = new Rol();
        rol.setNombre("usuario_free");

        pe.edu.utp.pasajeya.app.model.Persona persona = new pe.edu.utp.pasajeya.app.model.Persona();
        persona.setNombre("Ana");
        persona.setApellidoPaterno("Garcia");

        Usuario usuario = new Usuario();
        usuario.setEmail("ana@test.com");
        usuario.setPasswordHash(hashReal);
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setRol(rol);
        usuario.setPersona(persona);

        when(usuarioRepo.findByEmail("ana@test.com")).thenReturn(Optional.of(usuario));
        when(jwtUtil.generarToken("ana@test.com")).thenReturn("jwt-token-valido");

        LoginRequestDTO request = new LoginRequestDTO("ana@test.com", "Password123", "captcha");
        LoginResponseDTO respuesta = authService.login(request);

        assertThat(respuesta.token()).isEqualTo("jwt-token-valido");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 4: Contraseña incorrecta es rechazada por BCrypt (no por SQL)
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Contraseña incorrecta es rechazada por BCrypt.matches — no por SQL")
    void contrasenaIncorrecta_debeSerRechazadaPorBcrypt() {
        String hashReal = passwordEncoder.encode("Password123");

        Rol rol = new Rol();
        rol.setNombre("usuario_free");

        pe.edu.utp.pasajeya.app.model.Persona persona = new pe.edu.utp.pasajeya.app.model.Persona();
        persona.setNombre("Ana");
        persona.setApellidoPaterno("Garcia");

        Usuario usuario = new Usuario();
        usuario.setEmail("ana@test.com");
        usuario.setPasswordHash(hashReal);
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setRol(rol);
        usuario.setPersona(persona);

        when(usuarioRepo.findByEmail("ana@test.com")).thenReturn(Optional.of(usuario));

        LoginRequestDTO request = new LoginRequestDTO("ana@test.com", "contrasenaMal", "captcha");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Credenciales incorrectas");
    }
}
