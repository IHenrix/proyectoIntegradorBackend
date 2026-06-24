package pe.edu.utp.pasajeya.app.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSenderImpl mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "fromEmail", "no-reply@pasajeya.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:4200");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 1: Envia el correo de verificacion correctamente
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe enviar el correo de verificacion usando el mailSender")
    void cuandoEnviaVerificacion_debeUsarMailSender() {
        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.enviarVerificacion("ana@test.com", "token-123");

        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 2: Si el envio falla debe lanzar RuntimeException
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe lanzar excepcion cuando el envio del correo falla")
    void cuandoEnvioFalla_debeLanzarExcepcion() {
        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new org.springframework.mail.MailSendException("fallo de red"))
                .when(mailSender).send(mimeMessage);

        assertThatThrownBy(() -> emailService.enviarVerificacion("ana@test.com", "token-123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al enviar el correo");
    }
}
