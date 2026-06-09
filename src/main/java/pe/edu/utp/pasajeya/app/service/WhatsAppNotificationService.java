package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.model.Alerta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Service
public class WhatsAppNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppNotificationService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.twilio.enabled:false}")
    private boolean enabled;

    @Value("${app.twilio.account-sid:}")
    private String accountSid;

    @Value("${app.twilio.auth-token:}")
    private String authToken;

    @Value("${app.twilio.from-whatsapp:whatsapp:+14155238886}")
    private String fromWhatsapp;

    public void enviarAlertaPrecio(Alerta alerta, BigDecimal precioActual, double variacionPorcentual) {
        String body = """
                PasajeYa: encontramos una baja de precio.
                Ruta: %s -> %s
                Tarifa: %s
                Precio actual: S/ %.2f
                Variacion: %.1f%%
                """.formatted(
                alerta.getVuelo().getOrigen().trim(),
                alerta.getVuelo().getDestino().trim(),
                alerta.getTipoTarifa(),
                precioActual.doubleValue(),
                variacionPorcentual
        );

        if (!enabled || accountSid.isBlank() || authToken.isBlank()) {
            log.info("[TWILIO DEMO] WhatsApp a {}: {}", alerta.getTelefono(), body.replace("\n", " | "));
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(accountSid, authToken);

        MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
        payload.add("From", fromWhatsapp);
        payload.add("To", normalizarWhatsApp(alerta.getTelefono()));
        payload.add("Body", body);

        String url = "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json".formatted(accountSid);
        restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
        log.info("WhatsApp enviado para alerta {}", alerta.getId());
    }

    private String normalizarWhatsApp(String telefono) {
        String clean = telefono.trim();
        if (clean.startsWith("whatsapp:")) return clean;
        if (clean.startsWith("+")) return "whatsapp:" + clean;
        if (clean.startsWith("51")) return "whatsapp:+" + clean;
        return "whatsapp:+51" + clean;
    }
}
