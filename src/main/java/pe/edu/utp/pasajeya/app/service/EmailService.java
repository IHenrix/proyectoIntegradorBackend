package pe.edu.utp.pasajeya.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private static final int TIMEOUT_MS = 10_000;
    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";
    private static final String REMITENTE = "PasajeYa";

    private final RestClient restClient;

    @Value("${brevo.api-key}")
    private String brevoApiKey;

    @Value("${brevo.sender-email}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Autowired
    public EmailService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MS);
        factory.setReadTimeout(TIMEOUT_MS);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    // Constructor solo para tests: permite inyectar un RestClient mockeado
    // y no golpear la API real de Brevo durante mvn test.
    EmailService(RestClient restClient) {
        this.restClient = restClient;
    }

    public void enviarVerificacion(String destinatario, String token) {
        String urlVerificacion = baseUrl + "/api/auth/verificar?token=" + token;

        try {
            enviarHtml(destinatario, "PasajeYa — Verifica tu cuenta", buildHtmlEmail(urlVerificacion));
            log.info("Email de verificacion enviado a {}", destinatario);
        } catch (Exception e) {
            log.error("Error enviando email a {}: {}", destinatario, e.getMessage());
            throw new RuntimeException("Error al enviar el correo de verificación", e);
        }
    }

    private void enviarHtml(String destinatario, String asunto, String html) {
        Map<String, Object> body = Map.of(
                "sender", Map.of("name", REMITENTE, "email", fromEmail),
                "to", List.of(Map.of("email", destinatario)),
                "subject", asunto,
                "htmlContent", html
        );

        restClient.post()
                .uri(BREVO_URL)
                .header("api-key", brevoApiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(body)
                .retrieve()
                .body(String.class);
    }

    private String buildHtmlEmail(String urlVerificacion) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width,initial-scale=1.0">
            </head>
            <body style="margin:0;padding:0;background:#f1f5f9;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f1f5f9;padding:40px 16px;">
                <tr><td align="center">
                  <table width="560" cellpadding="0" cellspacing="0"
                         style="max-width:560px;width:100%%;background:#ffffff;border-radius:16px;
                                overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.10);">

                    <!-- Header -->
                    <tr>
                      <td style="background:#080d1a;padding:28px 40px;text-align:center;">
                        <p style="margin:0;font-size:26px;font-weight:700;color:#ffffff;letter-spacing:-0.5px;">
                          &#9992; Pasaje<span style="color:#fbbf24;">Ya</span>
                        </p>
                      </td>
                    </tr>

                    <!-- Body -->
                    <tr>
                      <td style="padding:40px 40px 32px;">
                        <h1 style="margin:0 0 12px;font-size:22px;font-weight:700;color:#0f172a;">
                          Verifica tu correo electr&#243;nico
                        </h1>
                        <p style="margin:0 0 28px;font-size:15px;color:#475569;line-height:1.7;">
                          &#161;Hola! Gracias por registrarte en <strong>PasajeYa</strong>.<br>
                          Para activar tu cuenta y empezar a comparar precios de vuelos nacionales,
                          confirma tu direcci&#243;n de correo haciendo clic en el bot&#243;n de abajo.
                        </p>

                        <!-- Button -->
                        <table width="100%%" cellpadding="0" cellspacing="0">
                          <tr>
                            <td align="center" style="padding:4px 0 36px;">
                              <a href="%s"
                                 style="display:inline-block;padding:15px 44px;
                                        background:linear-gradient(90deg,#f59e0b 0%%,#f97316 100%%);
                                        color:#ffffff;font-size:16px;font-weight:700;
                                        text-decoration:none;border-radius:12px;
                                        letter-spacing:0.02em;">
                                Validar correo &rarr;
                              </a>
                            </td>
                          </tr>
                        </table>

                        <p style="margin:0 0 6px;font-size:13px;color:#94a3b8;">
                          Si el bot&#243;n no funciona, copia y pega este enlace en tu navegador:
                        </p>
                        <p style="margin:0 0 28px;font-size:12px;color:#3b82f6;word-break:break-all;">
                          %s
                        </p>

                        <hr style="border:none;border-top:1px solid #e2e8f0;margin:0 0 24px;">

                        <p style="margin:0;font-size:12px;color:#94a3b8;line-height:1.7;">
                          &#9200; Este enlace expira en <strong>24 horas</strong>.<br>
                          Si no creaste una cuenta en PasajeYa, puedes ignorar este mensaje con tranquilidad.
                        </p>
                      </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                      <td style="background:#f8fafc;padding:20px 40px;text-align:center;
                                 border-top:1px solid #e2e8f0;">
                        <p style="margin:0;font-size:12px;color:#94a3b8;">
                          &copy; 2026 PasajeYa &middot; Compara vuelos nacionales en Per&#250;
                        </p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(urlVerificacion, urlVerificacion);
    }
}
