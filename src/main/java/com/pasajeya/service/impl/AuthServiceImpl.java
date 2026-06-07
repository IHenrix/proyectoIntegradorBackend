package com.pasajeya.service.impl;

import com.google.common.base.Preconditions;
import com.pasajeya.dto.LoginRequestDTO;
import com.pasajeya.dto.LoginResponseDTO;
import com.pasajeya.dto.RegistroRequestDTO;
import com.pasajeya.model.Persona;
import com.pasajeya.model.Rol;
import com.pasajeya.model.TokenVerificacion;
import com.pasajeya.model.Usuario;
import com.pasajeya.repository.RolRepository;
import com.pasajeya.repository.TokenVerificacionRepository;
import com.pasajeya.repository.UsuarioRepository;
import com.pasajeya.security.JwtUtil;
import com.pasajeya.service.AuthService;
import com.pasajeya.service.EmailService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UsuarioRepository usuarioRepo;
    private final TokenVerificacionRepository tokenRepo;
    private final RolRepository rolRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    public AuthServiceImpl(UsuarioRepository usuarioRepo,
                           TokenVerificacionRepository tokenRepo,
                           RolRepository rolRepo,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           EmailService emailService) {
        this.usuarioRepo   = usuarioRepo;
        this.tokenRepo     = tokenRepo;
        this.rolRepo       = rolRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil       = jwtUtil;
        this.emailService  = emailService;
    }

    @Override
    @Transactional
    public void registro(RegistroRequestDTO dto) {
        // Google Guava — Preconditions para validar entradas
        Preconditions.checkArgument(StringUtils.isNotBlank(dto.email()), "El email no puede estar vacio");
        Preconditions.checkArgument(StringUtils.isNotBlank(dto.password()), "La contrasena no puede estar vacia");
        Preconditions.checkArgument(dto.password().length() >= 6, "La contrasena debe tener al menos 6 caracteres");

        // Apache Commons — normalizar email a minusculas
        String emailNormalizado = StringUtils.lowerCase(StringUtils.trimToEmpty(dto.email()));
        log.info("[Guava+Commons] Email normalizado: {}", emailNormalizado);

        if (usuarioRepo.existsByEmail(emailNormalizado)) {
            throw new RuntimeException("El email ya esta registrado");
        }

        Persona persona = new Persona();
        // Apache Commons — capitalizar nombre y apellido
        persona.setNombre(StringUtils.capitalize(StringUtils.trimToEmpty(dto.nombre())));
        persona.setApellido(StringUtils.capitalize(StringUtils.trimToEmpty(dto.apellido())));
        persona.setTelefono(dto.telefono());
        persona.setFechaRegistro(LocalDateTime.now());

        Rol rol = rolRepo.findById(1)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));

        Usuario usuario = new Usuario();
        usuario.setPersona(persona);
        usuario.setRol(rol);
        usuario.setEmail(emailNormalizado);
        usuario.setPasswordHash(passwordEncoder.encode(dto.password()));
        usuario.setProveedor("email");
        usuario.setActivo(true);
        usuario.setEmailVerificado(false);

        usuarioRepo.save(usuario);

        String tokenStr = UUID.randomUUID().toString();
        TokenVerificacion tv = new TokenVerificacion();
        tv.setUsuario(usuario);
        tv.setToken(tokenStr);
        tv.setFechaExpiracion(LocalDateTime.now().plusHours(24));
        tv.setUsado(false);
        tv.setFechaCreacion(LocalDateTime.now());
        tokenRepo.save(tv);

        emailService.enviarVerificacion(dto.email(), tokenStr);
        log.info("Usuario registrado: {}", dto.email());
    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO dto) {
        Usuario usuario = usuarioRepo.findByEmail(dto.email())
                .orElseThrow(() -> new RuntimeException("Credenciales incorrectas"));

        if (!passwordEncoder.matches(dto.password(), usuario.getPasswordHash())) {
            throw new RuntimeException("Credenciales incorrectas");
        }

        if (!Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            throw new RuntimeException("Verifica tu correo antes de iniciar sesion");
        }

        String token = jwtUtil.generarToken(dto.email());
        log.info("Login exitoso: {}", dto.email());

        return new LoginResponseDTO(token, usuario.getPersona().getNombre(), usuario.getRol().getNombre());
    }

    @Override
    @Transactional
    public void verificarEmail(String tokenStr) {
        TokenVerificacion tv = tokenRepo.findByToken(tokenStr)
                .orElseThrow(() -> new RuntimeException("Token invalido"));

        if (Boolean.TRUE.equals(tv.getUsado())) {
            throw new RuntimeException("Token ya utilizado");
        }

        if (tv.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expirado");
        }

        tv.getUsuario().setEmailVerificado(true);
        tv.setUsado(true);
        usuarioRepo.save(tv.getUsuario());
        tokenRepo.save(tv);

        log.info("Email verificado: {}", tv.getUsuario().getEmail());
    }
}
