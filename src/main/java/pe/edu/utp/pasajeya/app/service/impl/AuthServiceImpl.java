package pe.edu.utp.pasajeya.app.service.impl;

import com.google.common.base.Preconditions;
import pe.edu.utp.pasajeya.app.dto.LoginRequestDTO;
import pe.edu.utp.pasajeya.app.dto.LoginResponseDTO;
import pe.edu.utp.pasajeya.app.dto.RegistroRequestDTO;
import pe.edu.utp.pasajeya.app.model.Persona;
import pe.edu.utp.pasajeya.app.model.Rol;
import pe.edu.utp.pasajeya.app.model.TokenVerificacion;
import pe.edu.utp.pasajeya.app.model.Usuario;
import pe.edu.utp.pasajeya.app.repository.PersonaRepository;
import pe.edu.utp.pasajeya.app.repository.RolRepository;
import pe.edu.utp.pasajeya.app.repository.TipoDocumentoRepository;
import pe.edu.utp.pasajeya.app.repository.TokenVerificacionRepository;
import pe.edu.utp.pasajeya.app.repository.UsuarioRepository;
import pe.edu.utp.pasajeya.app.security.JwtUtil;
import pe.edu.utp.pasajeya.app.service.AuthService;
import pe.edu.utp.pasajeya.app.service.EmailService;
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
    private final PersonaRepository personaRepo;
    private final TokenVerificacionRepository tokenRepo;
    private final RolRepository rolRepo;
    private final TipoDocumentoRepository tipoDocRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    public AuthServiceImpl(UsuarioRepository usuarioRepo,
                           PersonaRepository personaRepo,
                           TokenVerificacionRepository tokenRepo,
                           RolRepository rolRepo,
                           TipoDocumentoRepository tipoDocRepo,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           EmailService emailService) {
        this.usuarioRepo     = usuarioRepo;
        this.personaRepo     = personaRepo;
        this.tokenRepo       = tokenRepo;
        this.rolRepo         = rolRepo;
        this.tipoDocRepo     = tipoDocRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil         = jwtUtil;
        this.emailService    = emailService;
    }

    @Override
    @Transactional
    public void registro(RegistroRequestDTO dto) {
        Preconditions.checkArgument(StringUtils.isNotBlank(dto.email()), "El email no puede estar vacío");
        Preconditions.checkArgument(StringUtils.isNotBlank(dto.password()), "La contraseña no puede estar vacía");
        Preconditions.checkArgument(dto.password().length() >= 8, "La contraseña debe tener al menos 8 caracteres");

        String emailNormalizado = StringUtils.lowerCase(StringUtils.trimToEmpty(dto.email()));

        if (usuarioRepo.existsByEmail(emailNormalizado)) {
            throw new RuntimeException("El correo ya está registrado");
        }

        String nroDoc = StringUtils.trimToNull(dto.nroDocumento());
        if (nroDoc != null && personaRepo.existsByNroDocumento(nroDoc)) {
            throw new RuntimeException("El número de documento ya está registrado");
        }

        Persona persona = new Persona();
        persona.setNombre(StringUtils.capitalize(StringUtils.trimToEmpty(dto.nombre())));
        persona.setApellidoPaterno(StringUtils.capitalize(StringUtils.trimToEmpty(dto.apellidoPaterno())));
        if (StringUtils.isNotBlank(dto.apellidoMaterno())) {
            persona.setApellidoMaterno(StringUtils.capitalize(StringUtils.trimToEmpty(dto.apellidoMaterno())));
        }
        if (StringUtils.isNotBlank(dto.genero())) {
            persona.setGenero(dto.genero().trim().toUpperCase());
        }
        persona.setTelefono(StringUtils.trimToEmpty(dto.telefono()));
        if (dto.fechaNacimiento() != null && !dto.fechaNacimiento().isBlank()) {
            persona.setFechaNacimiento(java.time.LocalDate.parse(dto.fechaNacimiento()));
        }
        if (dto.tipoDocumentoId() != null) {
            tipoDocRepo.findById(dto.tipoDocumentoId()).ifPresent(persona::setTipoDocumento);
        }
        if (dto.nroDocumento() != null && !dto.nroDocumento().isBlank()) {
            persona.setNroDocumento(StringUtils.trimToEmpty(dto.nroDocumento()));
        }
        persona.setFechaRegistro(LocalDateTime.now());

        Rol rol = rolRepo.findByNombre("usuario_free")
                .orElseThrow(() -> new RuntimeException("Rol no encontrado en la base de datos"));

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

        try {
            emailService.enviarVerificacion(dto.email(), tokenStr);
        } catch (Exception e) {
            log.warn("No se pudo enviar email de bienvenida a {}: {}", dto.email(), e.getMessage());
        }

        log.info("Usuario registrado: {}", emailNormalizado);
    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO dto) {
        String emailNormalizado = StringUtils.lowerCase(StringUtils.trimToEmpty(dto.email()));

        Usuario usuario = usuarioRepo.findByEmail(emailNormalizado)
                .orElseThrow(() -> new RuntimeException("Credenciales incorrectas"));

        if (!passwordEncoder.matches(dto.password(), usuario.getPasswordHash())) {
            throw new RuntimeException("Credenciales incorrectas");
        }

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new RuntimeException("La cuenta está desactivada. Contacta al soporte.");
        }

        if (!Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            throw new RuntimeException("Debes verificar tu correo electrónico antes de ingresar. Revisa tu bandeja de entrada.");
        }

        String token = jwtUtil.generarToken(emailNormalizado);
        log.info("Login exitoso: {}", emailNormalizado);

        return new LoginResponseDTO(token, usuario.getPersona().getNombre(), usuario.getRol().getNombre());
    }

    @Override
    @Transactional
    public void verificarEmail(String tokenStr) {
        TokenVerificacion tv = tokenRepo.findByToken(tokenStr)
                .orElseThrow(() -> new RuntimeException("Token inválido"));

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
