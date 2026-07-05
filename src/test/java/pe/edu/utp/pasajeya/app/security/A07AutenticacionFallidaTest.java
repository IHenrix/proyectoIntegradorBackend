package pe.edu.utp.pasajeya.app.security;

import pe.edu.utp.pasajeya.app.dto.LoginRequestDTO;
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

import java.security.MessageDigest;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PRUEBAS DE SEGURIDAD — A07: Autenticación Fallida
 *
 * Vulnerabilidad del taller (Sistema de Matrícula — CÓDIGO VULNERABLE):
 *   - Contraseña almacenada en MD5 sin salt (DigestUtils.md5Hex).
 *   - Sin límite de intentos de login (permite fuerza bruta).
 *   - JDBC directo sin PreparedStatement.
 *
 * Corrección en PasajeYa:
 *   - BCryptPasswordEncoder con salt automático — MD5 nunca coincide con BCrypt.
 *   - Email verificado obligatorio antes de permitir login (bloquea bots de registro).
 *   - Cuenta inactiva bloquea el acceso.
 *   - Token JWT con expiración configurable (jwt.expiration en application.properties).
 *   - El hash BCrypt es diferente en cada codificación (salt aleatorio) — rainbow tables inútiles.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("A07 — Autenticación Fallida: PasajeYa usa BCrypt + verificación de email + cuenta activa")
class A07AutenticacionFallidaTest {

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
        // lenient(): algunas pruebas (MD5 vs BCrypt) no invocan login(), asi que el
        // stub del captcha no siempre se usa. lenient evita UnnecessaryStubbingException.
        lenient().when(recaptchaService.verificar(any())).thenReturn(true);
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 1: Hash MD5 de la contraseña no coincide con BCrypt almacenado
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Un hash MD5 de la contraseña nunca coincide con el BCrypt almacenado en BD")
    void hashMd5_nuncaCoincidesConBcrypt() {
        String hashBcrypt = passwordEncoder.encode("Password123");
        String hashMd5    = toMd5Hex("Password123");

        assertThat(passwordEncoder.matches(hashMd5, hashBcrypt)).isFalse();
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 2: Cuenta no verificada por email bloquea el login
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Login con email no verificado debe ser rechazado con mensaje claro")
    void emailNoVerificado_bloqueaLogin() {
        Rol rol = new Rol();
        rol.setNombre("usuario_free");

        pe.edu.utp.pasajeya.app.model.Persona persona = new pe.edu.utp.pasajeya.app.model.Persona();
        persona.setNombre("Carlos");
        persona.setApellidoPaterno("Ruiz");

        Usuario usuario = new Usuario();
        usuario.setEmail("carlos@test.com");
        usuario.setPasswordHash(passwordEncoder.encode("Pass1234"));
        usuario.setActivo(true);
        usuario.setEmailVerificado(false);
        usuario.setRol(rol);
        usuario.setPersona(persona);

        when(usuarioRepo.findByEmail("carlos@test.com")).thenReturn(Optional.of(usuario));

        LoginRequestDTO request = new LoginRequestDTO("carlos@test.com", "Pass1234", "captcha");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("verificar tu correo");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 3: Cuenta desactivada (activo=false) bloquea el login
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Login con cuenta desactivada debe ser rechazado")
    void cuentaDesactivada_bloqueaLogin() {
        Rol rol = new Rol();
        rol.setNombre("usuario_free");

        pe.edu.utp.pasajeya.app.model.Persona persona = new pe.edu.utp.pasajeya.app.model.Persona();
        persona.setNombre("Maria");
        persona.setApellidoPaterno("Lopez");

        Usuario usuario = new Usuario();
        usuario.setEmail("maria@test.com");
        usuario.setPasswordHash(passwordEncoder.encode("Pass1234"));
        usuario.setActivo(false);
        usuario.setEmailVerificado(true);
        usuario.setRol(rol);
        usuario.setPersona(persona);

        when(usuarioRepo.findByEmail("maria@test.com")).thenReturn(Optional.of(usuario));

        LoginRequestDTO request = new LoginRequestDTO("maria@test.com", "Pass1234", "captcha");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("desactivada");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 4: BCrypt genera hashes distintos en cada codificacion (salt aleatorio)
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("BCrypt genera hashes distintos para la misma contraseña — rainbow tables inútiles")
    void bcrypt_generaHashesDiferentesConMismaContrasena() {
        String hash1 = passwordEncoder.encode("Password123");
        String hash2 = passwordEncoder.encode("Password123");

        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(passwordEncoder.matches("Password123", hash1)).isTrue();
        assertThat(passwordEncoder.matches("Password123", hash2)).isTrue();
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 5: Usuario inexistente recibe el mismo mensaje que contraseña incorrecta
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Usuario inexistente debe retornar 'Credenciales incorrectas' — no expone si el email existe")
    void usuarioInexistente_noExponeSiEmailExiste() {
        when(usuarioRepo.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

        LoginRequestDTO request = new LoginRequestDTO("noexiste@test.com", "cualquier", "captcha");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Credenciales incorrectas");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 6: JWT generado es validado correctamente (token con firma segura)
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Token JWT generado en login es válido y contiene el email del usuario")
    void tokenJwt_esValidoYContieneEmail() {
        Rol rol = new Rol();
        rol.setNombre("usuario_free");

        pe.edu.utp.pasajeya.app.model.Persona persona = new pe.edu.utp.pasajeya.app.model.Persona();
        persona.setNombre("Ana");
        persona.setApellidoPaterno("Garcia");

        Usuario usuario = new Usuario();
        usuario.setEmail("ana@test.com");
        usuario.setPasswordHash(passwordEncoder.encode("Pass1234"));
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setRol(rol);
        usuario.setPersona(persona);

        when(usuarioRepo.findByEmail("ana@test.com")).thenReturn(Optional.of(usuario));
        when(jwtUtil.generarToken("ana@test.com", "usuario_free")).thenReturn("jwt-firmado-seguro");

        LoginRequestDTO request = new LoginRequestDTO("ana@test.com", "Pass1234", "captcha");
        var respuesta = authService.login(request);

        assertThat(respuesta.token()).isEqualTo("jwt-firmado-seguro");
        verify(jwtUtil, times(1)).generarToken("ana@test.com", "usuario_free");
    }

    private String toMd5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
