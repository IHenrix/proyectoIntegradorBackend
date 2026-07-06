package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.LoginRequestDTO;
import pe.edu.utp.pasajeya.app.dto.LoginResponseDTO;
import pe.edu.utp.pasajeya.app.dto.RegistroRequestDTO;
import pe.edu.utp.pasajeya.app.model.*;
import pe.edu.utp.pasajeya.app.repository.*;
import pe.edu.utp.pasajeya.app.security.JwtUtil;
import pe.edu.utp.pasajeya.app.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepo;
    @Mock
    private PersonaRepository personaRepo;
    @Mock
    private TokenVerificacionRepository tokenRepo;
    @Mock
    private RolRepository rolRepo;
    @Mock
    private TipoDocumentoRepository tipoDocRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private EmailService emailService;
    @Mock
    private RecaptchaService recaptchaService;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegistroRequestDTO registroValido;
    private Rol rolFree;
    private Usuario usuario;
    private Persona persona;

    @BeforeEach
    void setUp() {
        registroValido = new RegistroRequestDTO(
                "Ana", "Garcia", null, "F",
                "ana@test.com", "password123", "987654321",
                null, null, null, "captcha-valido"
        );

        rolFree = new Rol();
        rolFree.setId(1);
        rolFree.setNombre("usuario_free");

        persona = new Persona();
        persona.setNombre("Ana");
        persona.setTelefono("+51999888777");

        usuario = new Usuario();
        usuario.setId(1);
        usuario.setEmail("ana@test.com");
        usuario.setPasswordHash("hash-encriptado");
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setPersona(persona);
        usuario.setRol(rolFree);
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 1: Registro exitoso
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe registrar un usuario correctamente cuando los datos son validos")
    void cuandoDatosValidos_debeRegistrarUsuario() {
        when(recaptchaService.verificar("captcha-valido")).thenReturn(true);
        when(usuarioRepo.existsByEmail("ana@test.com")).thenReturn(false);
        when(rolRepo.findByNombre("usuario_free")).thenReturn(Optional.of(rolFree));
        when(passwordEncoder.encode("password123")).thenReturn("hash-encriptado");

        authService.registro(registroValido);

        verify(usuarioRepo, times(1)).save(any(Usuario.class));
        verify(tokenRepo, times(1)).save(any(TokenVerificacion.class));
        verify(emailService, times(1)).enviarVerificacion(eq("ana@test.com"), anyString());
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 2: Captcha invalido debe lanzar excepcion
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe lanzar excepcion cuando el captcha es invalido")
    void cuandoCaptchaInvalido_debeLanzarExcepcion() {
        when(recaptchaService.verificar("captcha-valido")).thenReturn(false);

        assertThatThrownBy(() -> authService.registro(registroValido))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("captcha");

        verify(usuarioRepo, never()).save(any(Usuario.class));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 3: Email ya registrado debe lanzar excepcion
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe lanzar excepcion cuando el email ya esta registrado")
    void cuandoEmailYaRegistrado_debeLanzarExcepcion() {
        when(recaptchaService.verificar("captcha-valido")).thenReturn(true);
        when(usuarioRepo.existsByEmail("ana@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registro(registroValido))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ya está registrado");

        verify(usuarioRepo, never()).save(any(Usuario.class));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 4: Password muy corta debe lanzar excepcion
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe lanzar excepcion cuando la contrasena tiene menos de 8 caracteres")
    void cuandoPasswordMuyCorta_debeLanzarExcepcion() {
        RegistroRequestDTO registroPasswordCorta = new RegistroRequestDTO(
                "Ana", "Garcia", null, "F",
                "ana@test.com", "1234567", "987654321",
                null, null, null, "captcha-valido"
        );
        when(recaptchaService.verificar("captcha-valido")).thenReturn(true);

        assertThatThrownBy(() -> authService.registro(registroPasswordCorta))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 5: Login exitoso
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe iniciar sesion correctamente con credenciales validas")
    void cuandoCredencialesValidas_debeIniciarSesion() {
        LoginRequestDTO loginValido = new LoginRequestDTO("ana@test.com", "password123", "captcha-valido");
        when(recaptchaService.verificar("captcha-valido")).thenReturn(true);
        when(usuarioRepo.findByEmail("ana@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password123", "hash-encriptado")).thenReturn(true);
        when(jwtUtil.generarToken("ana@test.com", "usuario_free")).thenReturn("jwt-token-generado");

        LoginResponseDTO resultado = authService.login(loginValido);

        assertThat(resultado.token()).isEqualTo("jwt-token-generado");
        assertThat(resultado.nombre()).isEqualTo("Ana");
        assertThat(resultado.rol()).isEqualTo("usuario_free");
        assertThat(resultado.telefono()).isEqualTo("+51999888777");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 6: Login con password incorrecta debe lanzar excepcion
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe lanzar excepcion cuando la contrasena es incorrecta")
    void cuandoPasswordIncorrecta_debeLanzarExcepcion() {
        LoginRequestDTO loginInvalido = new LoginRequestDTO("ana@test.com", "passwordmala", "captcha-valido");
        when(recaptchaService.verificar("captcha-valido")).thenReturn(true);
        when(usuarioRepo.findByEmail("ana@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("passwordmala", "hash-encriptado")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginInvalido))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Credenciales incorrectas");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 7: Login con cuenta sin verificar debe lanzar excepcion
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe lanzar excepcion cuando el correo no ha sido verificado")
    void cuandoEmailNoVerificado_debeLanzarExcepcion() {
        usuario.setEmailVerificado(false);
        LoginRequestDTO loginValido = new LoginRequestDTO("ana@test.com", "password123", "captcha-valido");
        when(recaptchaService.verificar("captcha-valido")).thenReturn(true);
        when(usuarioRepo.findByEmail("ana@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password123", "hash-encriptado")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(loginValido))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("verificar tu correo");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 8: Login con cuenta desactivada debe lanzar excepcion
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe lanzar excepcion cuando la cuenta esta desactivada")
    void cuandoCuentaDesactivada_debeLanzarExcepcion() {
        usuario.setActivo(false);
        LoginRequestDTO loginValido = new LoginRequestDTO("ana@test.com", "password123", "captcha-valido");
        when(recaptchaService.verificar("captcha-valido")).thenReturn(true);
        when(usuarioRepo.findByEmail("ana@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password123", "hash-encriptado")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(loginValido))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("desactivada");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 9: Verificar email con token valido
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe verificar el email correctamente con un token valido")
    void cuandoTokenValido_debeVerificarEmail() {
        TokenVerificacion tv = new TokenVerificacion();
        tv.setToken("token-valido");
        tv.setUsado(false);
        tv.setFechaExpiracion(LocalDateTime.now().plusHours(1));
        tv.setUsuario(usuario);
        when(tokenRepo.findByToken("token-valido")).thenReturn(Optional.of(tv));

        authService.verificarEmail("token-valido");

        assertThat(usuario.getEmailVerificado()).isTrue();
        assertThat(tv.getUsado()).isTrue();
        verify(usuarioRepo, times(1)).save(usuario);
        verify(tokenRepo, times(1)).save(tv);
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 10: Token ya usado debe lanzar excepcion
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe lanzar excepcion cuando el token ya fue utilizado")
    void cuandoTokenYaUsado_debeLanzarExcepcion() {
        TokenVerificacion tv = new TokenVerificacion();
        tv.setToken("token-usado");
        tv.setUsado(true);
        when(tokenRepo.findByToken("token-usado")).thenReturn(Optional.of(tv));

        assertThatThrownBy(() -> authService.verificarEmail("token-usado"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ya utilizado");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 11: Token expirado debe lanzar excepcion
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe lanzar excepcion cuando el token esta expirado")
    void cuandoTokenExpirado_debeLanzarExcepcion() {
        TokenVerificacion tv = new TokenVerificacion();
        tv.setToken("token-expirado");
        tv.setUsado(false);
        tv.setFechaExpiracion(LocalDateTime.now().minusHours(1));
        when(tokenRepo.findByToken("token-expirado")).thenReturn(Optional.of(tv));

        assertThatThrownBy(() -> authService.verificarEmail("token-expirado"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expirado");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 12: Documento ya registrado debe lanzar excepcion
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe lanzar excepcion cuando el numero de documento ya esta registrado")
    void cuandoDocumentoYaRegistrado_debeLanzarExcepcion() {
        RegistroRequestDTO registroConDoc = new RegistroRequestDTO(
                "Ana", "Garcia", null, "F",
                "ana@test.com", "password123", "987654321",
                null, null, "12345678", "captcha-valido"
        );
        when(recaptchaService.verificar("captcha-valido")).thenReturn(true);
        when(usuarioRepo.existsByEmail("ana@test.com")).thenReturn(false);
        when(personaRepo.existsByNroDocumento("12345678")).thenReturn(true);

        assertThatThrownBy(() -> authService.registro(registroConDoc))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("documento ya está registrado");

        verify(usuarioRepo, never()).save(any(Usuario.class));
    }
}
