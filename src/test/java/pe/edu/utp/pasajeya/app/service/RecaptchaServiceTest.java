package pe.edu.utp.pasajeya.app.service;

import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

class RecaptchaServiceTest {

    private RecaptchaService recaptchaService;

    @BeforeEach
    void setUp() {
        recaptchaService = new RecaptchaService();
        ReflectionTestUtils.setField(recaptchaService, "secret", "secret-de-prueba");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 1: Cuando esta deshabilitado, siempre aprueba sin llamar a Google
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe aprobar siempre cuando la verificacion de captcha esta deshabilitada")
    void cuandoEstaDeshabilitado_debeAprobarSiempre() {
        ReflectionTestUtils.setField(recaptchaService, "enabled", false);

        boolean resultado = recaptchaService.verificar(null);

        assertThat(resultado).isTrue();
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 2: Token vacio debe rechazar sin necesidad de llamar a Google
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe rechazar cuando el token esta vacio")
    void cuandoTokenVacio_debeRechazar() {
        ReflectionTestUtils.setField(recaptchaService, "enabled", true);

        boolean resultado = recaptchaService.verificar("");

        assertThat(resultado).isFalse();
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 3: Token nulo debe rechazar sin necesidad de llamar a Google
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe rechazar cuando el token es nulo")
    void cuandoTokenNulo_debeRechazar() {
        ReflectionTestUtils.setField(recaptchaService, "enabled", true);

        boolean resultado = recaptchaService.verificar(null);

        assertThat(resultado).isFalse();
    }
}
