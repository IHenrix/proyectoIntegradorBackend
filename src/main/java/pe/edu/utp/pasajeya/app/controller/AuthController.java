package pe.edu.utp.pasajeya.app.controller;

import pe.edu.utp.pasajeya.app.dto.LoginRequestDTO;
import pe.edu.utp.pasajeya.app.dto.LoginResponseDTO;
import pe.edu.utp.pasajeya.app.dto.RegistroRequestDTO;
import pe.edu.utp.pasajeya.app.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    public ResponseEntity<String> registro(@RequestBody @Valid RegistroRequestDTO dto) {
        authService.registro(dto);
        log.info("POST /api/auth/registro → {}", dto.email());
        return ResponseEntity.ok("Registro exitoso. Revisa tu correo para verificar tu cuenta.");
    }

    @GetMapping("/verificar")
    public ResponseEntity<Void> verificar(@RequestParam String token) {
        try {
            authService.verificarEmail(token);
            log.info("Email verificado con token: {}", token);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/auth?verificado=ok"))
                    .build();
        } catch (Exception e) {
            log.warn("Verificacion fallida: {}", e.getMessage());
            String msg = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/auth?verificado=error&msg=" + msg))
                    .build();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO dto) {
        LoginResponseDTO response = authService.login(dto);
        log.info("POST /api/auth/login → {}", dto.email());
        return ResponseEntity.ok(response);
    }
}
