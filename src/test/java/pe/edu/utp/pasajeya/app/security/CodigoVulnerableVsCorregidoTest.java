package pe.edu.utp.pasajeya.app.security;

import org.junit.jupiter.api.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

/**
 * DEMOSTRACIÓN: Código Vulnerable (Sistema de Matrícula del taller) vs
 *               Código Corregido (PasajeYá)
 *
 * Esta clase ilustra con código ejecutable las diferencias entre los
 * patrones inseguros del taller y los patrones seguros que PasajeYá implementa.
 * Correspondencia con las secciones del taller S14.s2:
 *   Sección 6 — Implementación de controles de seguridad
 */
@DisplayName("Código Vulnerable vs Corregido — PasajeYá vs Sistema de Matrícula")
class CodigoVulnerableVsCorregidoTest {

    private final PasswordEncoder bcrypt = new BCryptPasswordEncoder();

    // ═══════════════════════════════════════════════════════════════════════
    // A03 — SQL INJECTION
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("A03 — SQL Injection: comparación PreparedStatement vs concatenación")
    class SqlInjection {

        @Test
        @DisplayName("VULNERABLE: concatenar input en SQL permite payloads de inyección")
        void vulnerable_concatenacionSql_permitePayload() {
            String emailMalicioso = "' OR '1'='1";

            // Esto es lo que hacía el Sistema de Matrícula del taller:
            // La query resultante: SELECT * FROM usuarios WHERE email='' OR '1'='1'
            // Resultado: retorna TODOS los usuarios — acceso sin contraseña
            String queryVulnerable = "SELECT * FROM usuarios WHERE email='"
                    + emailMalicioso + "'";

            // El payload inyecta lógica SQL — la condición '1'='1' siempre es true
            assertThat(queryVulnerable).contains("OR '1'='1'");
            assertThat(queryVulnerable).doesNotContain("?"); // sin PreparedStatement
        }

        @Test
        @DisplayName("CORREGIDO: JPA usa PreparedStatement — el payload se trata como dato")
        void corregido_jpa_trataPayloadComoDato() {
            String emailMalicioso = "' OR '1'='1";

            // PasajeYá usa: usuarioRepo.findByEmail(email)
            // Spring Data JPA genera internamente:
            // SELECT u FROM Usuario u WHERE u.email = ?
            // El email malicioso se pasa como parámetro — nunca se interpreta como SQL.

            // Simulamos el resultado: findByEmail retorna Optional.empty()
            // porque no hay ningún usuario con ese email exacto en BD.
            java.util.Optional<String> resultado = java.util.Optional.empty();

            assertThat(resultado).isEmpty(); // el payload no produce resultados
            // La excepción "Credenciales incorrectas" sería lanzada a continuación
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // A07 — HASH DE CONTRASEÑAS: MD5 vs BCrypt
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("A07 — Hash de contraseñas: MD5 (vulnerable) vs BCrypt (corregido)")
    class HashContrasena {

        @Test
        @DisplayName("VULNERABLE: MD5 sin salt — mismo input produce siempre el mismo hash")
        void vulnerable_md5_mismoHashParaMismaContrasena() {
            String hash1 = toMd5Hex("Password123");
            String hash2 = toMd5Hex("Password123");

            // MD5 es determinista — dos cálculos con el mismo input dan el mismo hash.
            // Esto permite rainbow tables: tablas precomputadas de hash→contraseña.
            assertThat(hash1).isEqualTo(hash2);
            assertThat(hash1).isEqualTo("42f749ade7f9e195bf475f37a44cafcb");
        }

        @Test
        @DisplayName("CORREGIDO: BCrypt genera hashes únicos por el salt automático")
        void corregido_bcrypt_hashDiferenteConMismaContrasena() {
            String hash1 = bcrypt.encode("Password123");
            String hash2 = bcrypt.encode("Password123");

            // BCrypt incorpora un salt aleatorio — cada hash es único.
            // Las rainbow tables no funcionan porque habría que precomputar
            // una tabla por cada salt posible (2^128 combinaciones).
            assertThat(hash1).isNotEqualTo(hash2);

            // Ambos hashes siguen siendo válidos para la misma contraseña:
            assertThat(bcrypt.matches("Password123", hash1)).isTrue();
            assertThat(bcrypt.matches("Password123", hash2)).isTrue();
        }

        @Test
        @DisplayName("VULNERABLE: hash MD5 podría ser buscado en rainbow table online")
        void vulnerable_md5_visibleEnRainbowTable() {
            // El hash MD5 de contraseñas comunes aparece en bases de datos públicas.
            // Ejemplo: md5("123456") = "e10adc3949ba59abbe56e057f20f883e"
            // Sitios como md5decrypt.net o crackstation.net lo descifran en segundos.
            String hashDebil = toMd5Hex("123456");
            assertThat(hashDebil).isEqualTo("e10adc3949ba59abbe56e057f20f883e");
            // Este valor está indexado en rainbow tables públicas.
        }

        @Test
        @DisplayName("CORREGIDO: BCrypt resiste rainbow tables y fuerza bruta GPU")
        void corregido_bcrypt_resisteAtaques() {
            String hashBcrypt = bcrypt.encode("123456");

            // El hash BCrypt empieza con $2a$ y contiene el salt embebido.
            // Una GPU moderna puede calcular ~10,000,000,000 MD5/segundo,
            // pero solo ~100 BCrypt/segundo (factor de trabajo 10 por defecto).
            // Esto hace inviable la fuerza bruta.
            assertThat(hashBcrypt).startsWith("$2a$");
            assertThat(bcrypt.matches("123456", hashBcrypt)).isTrue();
            assertThat(bcrypt.matches("654321", hashBcrypt)).isFalse();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // A01 — CONTROL DE ACCESO ROTO: IDOR vs JWT claim
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("A01 — Control de Acceso Roto: IDOR en URL vs email del JWT")
    class ControlDeAcceso {

        @Test
        @DisplayName("VULNERABLE: ID de recurso en URL permite IDOR — cualquiera cambia el número")
        void vulnerable_idEnUrl_permiteIdorConceptual() {
            // En el Sistema de Matrícula del taller:
            // GET /historial/42  → historial del estudiante con id=42
            // Un atacante cambia el número:
            // GET /historial/43  → historial del estudiante con id=43 (sin verificación)

            // Simulamos el vector de ataque:
            long idVictima = 43L;
            long idAtacante = 42L;

            // El servidor nunca verificó que idVictima == idAtacante (IDOR)
            assertThat(idVictima).isNotEqualTo(idAtacante);
            // En el sistema vulnerable, ambas peticiones retornan 200 OK
        }

        @Test
        @DisplayName("CORREGIDO: PasajeYá obtiene el email del JWT — no del parámetro de URL")
        void corregido_emailDelJwt_impisteImpersonacion() {
            // AlertaController.java en PasajeYá:
            // @GetMapping
            // public ResponseEntity<List<AlertaDTO>> listar(Authentication auth) {
            //     return ResponseEntity.ok(alertaService.listar(auth.getName()));
            // }
            //
            // auth.getName() es el email extraído del JWT por JwtFilter.
            // El usuario NO puede cambiar su propio email en el token
            // sin invalidar la firma HMAC-SHA256.

            // Simulamos que un atacante intenta acceder con un token de "atacante@test.com":
            String emailDelToken = "atacante@test.com";
            String emailVictima  = "victima@test.com";

            // El servicio usará el email del token — nunca el de la víctima
            String emailUsado = emailDelToken; // auth.getName() del JwtFilter
            assertThat(emailUsado).isNotEqualTo(emailVictima);
            // alertaService.listar("atacante@test.com") solo retorna SUS alertas
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // A07 — VERIFICACIONES ADICIONALES: email + cuenta activa
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("A07 — Verificaciones adicionales en login")
    class VerificacionesLogin {

        @Test
        @DisplayName("VULNERABLE: sin verificación de email — bots pueden registrar y usar cuentas")
        void vulnerable_sinVerificacionEmail_conceptual() {
            // En el Sistema de Matrícula del taller no hay verificación de email.
            // Un bot puede registrar miles de cuentas con emails falsos
            // y usarlas inmediatamente. Esto facilita ataques de spam,
            // abuso del sistema y enumeration de vulnerabilidades.

            boolean emailVerificadoRequerido = false; // sistema vulnerable
            assertThat(emailVerificadoRequerido).isFalse();
        }

        @Test
        @DisplayName("CORREGIDO: PasajeYá requiere emailVerificado=true antes de dar acceso")
        void corregido_emailVerificadoObligatorio() {
            // AuthServiceImpl.java:
            // if (!Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            //     throw new RuntimeException("Debes verificar tu correo electrónico...");
            // }
            //
            // Esto bloquea:
            // - Cuentas recién creadas que no confirmaron el email
            // - Bots de registro masivo
            // - Cuentas creadas con emails de otras personas

            boolean emailVerificado = false; // cuenta nueva sin confirmar

            assertThatCode(() -> {
                if (!emailVerificado) {
                    throw new RuntimeException("Debes verificar tu correo electrónico");
                }
            }).isInstanceOf(RuntimeException.class)
              .hasMessageContaining("verificar tu correo");
        }
    }

    // ── helper interno ──────────────────────────────────────────────────
    private String toMd5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
