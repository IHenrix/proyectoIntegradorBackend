package pe.edu.utp.pasajeya.app.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que el rol viaje como claim FIRMADO dentro del JWT — la fuente de
 * verdad de autorización ya no puede ser algo que el cliente controle
 * (como localStorage), sino un valor que solo el servidor pudo haber escrito,
 * porque está protegido por la firma HMAC del token.
 */
@DisplayName("JwtUtil — el rol viaja firmado dentro del token, no es editable por el cliente")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Mismo formato Base64 que application.properties (jwt.secret), pero
        // un valor propio de prueba para no acoplar el test a la config real.
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
    }

    @Test
    @DisplayName("generarToken() incluye el rol como claim y extraerRol() lo recupera igual")
    void generarToken_incluyeClaimRol() {
        String token = jwtUtil.generarToken("admin@pasajeya.com.pe", "admin");

        assertThat(jwtUtil.extraerEmail(token)).isEqualTo("admin@pasajeya.com.pe");
        assertThat(jwtUtil.extraerRol(token)).isEqualTo("admin");
    }

    @Test
    @DisplayName("generarToken() con distintos roles produce claims distintos y verificables")
    void generarToken_conDistintosRoles() {
        String tokenFree    = jwtUtil.generarToken("free@test.com", "usuario_free");
        String tokenPremium = jwtUtil.generarToken("premium@test.com", "usuario_premium");

        assertThat(jwtUtil.extraerRol(tokenFree)).isEqualTo("usuario_free");
        assertThat(jwtUtil.extraerRol(tokenPremium)).isEqualTo("usuario_premium");
    }

    @Test
    @DisplayName("validar() acepta un token recién generado con rol")
    void validar_tokenConRol_esValido() {
        String token = jwtUtil.generarToken("ana@test.com", "usuario_free");
        assertThat(jwtUtil.validar(token)).isTrue();
    }

    @Test
    @DisplayName("validar() rechaza un token manipulado (firma inválida)")
    void validar_tokenManipulado_esInvalido() {
        String token = jwtUtil.generarToken("ana@test.com", "usuario_free");
        // Cambia el último caracter de la firma — simula que alguien intentó
        // alterar el token sin conocer la clave secreta del servidor.
        String tokenManipulado = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");

        assertThat(jwtUtil.validar(tokenManipulado)).isFalse();
    }
}
