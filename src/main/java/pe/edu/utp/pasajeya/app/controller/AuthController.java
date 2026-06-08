package pe.edu.utp.pasajeya.app.controller;

import pe.edu.utp.pasajeya.app.dto.LoginRequestDTO;
import pe.edu.utp.pasajeya.app.dto.LoginResponseDTO;
import pe.edu.utp.pasajeya.app.dto.RegistroRequestDTO;
import pe.edu.utp.pasajeya.app.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    public ResponseEntity<String> registro(@RequestBody @Valid RegistroRequestDTO dto) {
        authService.registro(dto);
        log.info("POST /api/auth/registro → {}", dto.email());
        return ResponseEntity.ok("Registro exitoso. Revisa tu email para verificar tu cuenta.");
    }

    @GetMapping("/verificar")
    public ResponseEntity<String> verificar(@RequestParam String token) {
        authService.verificarEmail(token);
        return ResponseEntity.ok("Cuenta verificada. Ya puedes iniciar sesion.");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO dto) {
        LoginResponseDTO response = authService.login(dto);
        log.info("POST /api/auth/login → {}", dto.email());
        return ResponseEntity.ok(response);
    }
}
