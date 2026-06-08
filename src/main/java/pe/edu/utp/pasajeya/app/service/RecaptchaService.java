package pe.edu.utp.pasajeya.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class RecaptchaService {

    private static final Logger log = LoggerFactory.getLogger(RecaptchaService.class);
    private static final String VERIFY_URL =
            "https://www.google.com/recaptcha/api/siteverify?secret=%s&response=%s";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.recaptcha.secret}")
    private String secret;

    @Value("${app.recaptcha.enabled:true}")
    private boolean enabled;

    @SuppressWarnings("unchecked")
    public boolean verificar(String token) {
        if (!enabled) return true;
        if (token == null || token.isBlank()) {
            log.warn("Captcha token vacio");
            return false;
        }
        try {
            String url = VERIFY_URL.formatted(secret, token);
            Map<String, Object> response = restTemplate.postForObject(url, null, Map.class);
            boolean success = response != null && Boolean.TRUE.equals(response.get("success"));
            if (!success) log.warn("Captcha fallido: {}", response);
            return success;
        } catch (Exception e) {
            log.error("Error verificando captcha: {}", e.getMessage());
            return false;
        }
    }
}
