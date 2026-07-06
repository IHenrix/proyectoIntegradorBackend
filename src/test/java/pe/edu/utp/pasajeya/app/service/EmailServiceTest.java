package pe.edu.utp.pasajeya.app.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private RestClient restClient;
    @Mock private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private RestClient.RequestBodySpec requestBodySpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(restClient);
        ReflectionTestUtils.setField(emailService, "brevoApiKey", "clave-de-prueba");
        ReflectionTestUtils.setField(emailService, "fromEmail", "no-reply@pasajeya.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:4200");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 1: Envia el correo de verificacion via Brevo correctamente
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe enviar el correo de verificacion via Brevo con los datos correctos")
    void cuandoEnviaVerificacion_debeLlamarABrevo() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("https://api.brevo.com/v3/smtp/email")).thenReturn(requestBodySpec);
        when(requestBodySpec.header(eq("api-key"), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(eq("Content-Type"), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(eq("Accept"), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn("{\"messageId\":\"abc\"}");

        emailService.enviarVerificacion("ana@test.com", "token-123");

        verify(restClient, times(1)).post();
        verify(requestBodyUriSpec).uri("https://api.brevo.com/v3/smtp/email");
        verify(requestBodySpec).header("api-key", "clave-de-prueba");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 2: Si Brevo falla (red, 4xx, 5xx), debe lanzar RuntimeException
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe lanzar excepcion cuando el envio via Brevo falla")
    void cuandoEnvioFalla_debeLanzarExcepcion() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenThrow(new RestClientException("fallo de red"));

        assertThatThrownBy(() -> emailService.enviarVerificacion("ana@test.com", "token-123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al enviar el correo")
                .hasCauseInstanceOf(RestClientException.class);
    }
}
