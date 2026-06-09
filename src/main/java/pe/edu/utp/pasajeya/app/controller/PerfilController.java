package pe.edu.utp.pasajeya.app.controller;

import pe.edu.utp.pasajeya.app.dto.ActualizarPerfilRequestDTO;
import pe.edu.utp.pasajeya.app.dto.PerfilDTO;
import pe.edu.utp.pasajeya.app.model.Persona;
import pe.edu.utp.pasajeya.app.model.TipoDocumento;
import pe.edu.utp.pasajeya.app.model.Usuario;
import pe.edu.utp.pasajeya.app.repository.PersonaRepository;
import pe.edu.utp.pasajeya.app.repository.TipoDocumentoRepository;
import pe.edu.utp.pasajeya.app.repository.UsuarioRepository;
import pe.edu.utp.pasajeya.app.security.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final UsuarioRepository       usuarioRepo;
    private final PersonaRepository       personaRepo;
    private final TipoDocumentoRepository tipoDocRepo;
    private final JwtUtil                 jwtUtil;
    private final PasswordEncoder         passwordEncoder;

    public PerfilController(UsuarioRepository usuarioRepo,
                            PersonaRepository personaRepo,
                            TipoDocumentoRepository tipoDocRepo,
                            JwtUtil jwtUtil,
                            PasswordEncoder passwordEncoder) {
        this.usuarioRepo     = usuarioRepo;
        this.personaRepo     = personaRepo;
        this.tipoDocRepo     = tipoDocRepo;
        this.jwtUtil         = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    /** GET /api/perfil */
    @GetMapping
    public ResponseEntity<PerfilDTO> obtener(@RequestHeader("Authorization") String authHeader) {
        Usuario u = resolverUsuario(authHeader);
        Persona p = u.getPersona();
        return ResponseEntity.ok(toDTO(u, p));
    }

    /** PUT /api/perfil */
    @PutMapping
    public ResponseEntity<PerfilDTO> actualizar(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ActualizarPerfilRequestDTO dto) {

        Usuario u = resolverUsuario(authHeader);
        Persona p = u.getPersona();

        // Nombre y apellidos
        if (StringUtils.isNotBlank(dto.nombre()))
            p.setNombre(StringUtils.capitalize(dto.nombre().trim()));
        if (StringUtils.isNotBlank(dto.apellidoPaterno()))
            p.setApellidoPaterno(StringUtils.capitalize(dto.apellidoPaterno().trim()));
        p.setApellidoMaterno(StringUtils.isNotBlank(dto.apellidoMaterno())
                ? StringUtils.capitalize(dto.apellidoMaterno().trim()) : null);

        // Género y teléfono
        if (StringUtils.isNotBlank(dto.genero()))
            p.setGenero(dto.genero().trim().toUpperCase());
        p.setTelefono(StringUtils.trimToNull(dto.telefono()));

        // Fecha nacimiento
        if (StringUtils.isNotBlank(dto.fechaNacimiento()))
            p.setFechaNacimiento(java.time.LocalDate.parse(dto.fechaNacimiento()));

        // Documento de identidad
        if (StringUtils.isNotBlank(dto.nroDocumento())) {
            String nroNuevo = dto.nroDocumento().trim();

            // Validar duplicado excluyendo la persona actual
            if (personaRepo.existsByNroDocumentoAndIdNot(nroNuevo, p.getId()))
                throw new RuntimeException("El número de documento ya está registrado por otro usuario");

            // Asignar tipo de documento
            if (StringUtils.isNotBlank(dto.tipoDocumento())) {
                TipoDocumento tipo = tipoDocRepo.findByCodigo(dto.tipoDocumento().trim().toUpperCase())
                        .orElseThrow(() -> new RuntimeException("Tipo de documento no válido: " + dto.tipoDocumento()));
                p.setTipoDocumento(tipo);
            }
            p.setNroDocumento(nroNuevo);
        } else if (dto.nroDocumento() != null && dto.nroDocumento().isBlank()) {
            // Si manda cadena vacía, limpiar
            p.setNroDocumento(null);
            p.setTipoDocumento(null);
        }

        // Cambio de contraseña
        if (StringUtils.isNotBlank(dto.passwordActual()) && StringUtils.isNotBlank(dto.passwordNuevo())) {
            if (!passwordEncoder.matches(dto.passwordActual(), u.getPasswordHash()))
                throw new RuntimeException("La contraseña actual es incorrecta");
            if (dto.passwordNuevo().length() < 8)
                throw new RuntimeException("La nueva contraseña debe tener al menos 8 caracteres");
            u.setPasswordHash(passwordEncoder.encode(dto.passwordNuevo()));
        }

        usuarioRepo.save(u);
        return ResponseEntity.ok(toDTO(u, p));
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private PerfilDTO toDTO(Usuario u, Persona p) {
        return new PerfilDTO(
                p.getNombre(),
                p.getApellidoPaterno(),
                p.getApellidoMaterno(),
                p.getGenero(),
                p.getTelefono(),
                p.getFechaNacimiento() != null ? p.getFechaNacimiento().toString() : null,
                u.getEmail(),
                u.getRol().getNombre(),
                p.getTipoDocumento() != null ? p.getTipoDocumento().getCodigo() : null,
                p.getNroDocumento()
        );
    }

    private Usuario resolverUsuario(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.extraerEmail(token);
        return usuarioRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}
