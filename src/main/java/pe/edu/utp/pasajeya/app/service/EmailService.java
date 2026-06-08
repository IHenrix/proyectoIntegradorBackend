package pe.edu.utp.pasajeya.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarVerificacion(String destinatario, String token) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(from);
        mensaje.setTo(destinatario);
        mensaje.setSubject("PasajeYa — Verifica tu cuenta");
        mensaje.setText(
            "Hola,\n\n" +
            "Haz clic en el siguiente enlace para verificar tu cuenta:\n\n" +
            "http://localhost:8080/api/auth/verificar?token=" + token + "\n\n" +
            "El enlace expira en 24 horas.\n\n" +
            "PasajeYa"
        );
        mailSender.send(mensaje);
        log.info("Email de verificacion enviado a {}", destinatario);
    }
}
