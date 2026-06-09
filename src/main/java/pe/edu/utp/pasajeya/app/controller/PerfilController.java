package pe.edu.utp.pasajeya.app.controller;

import pe.edu.utp.pasajeya.app.dto.ActualizarPerfilRequestDTO;
import pe.edu.utp.pasajeya.app.dto.PerfilDTO;
import pe.edu.utp.pasajeya.app.model.Persona;
import pe.edu.utp.pasajeya.app.model.Usuario;
import pe.edu.utp.pasajeya.app.repository.UsuarioRepository;
import pe.edu.utp.pasajeya.app.security.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final UsuarioRepository usuarioRepo;
    private final JwtUtil           jwtUtil;
    private final PasswordEncoder   passwordEncoder;

    public PerfilController(UsuarioRepository usuarioRepo,
                            JwtUtil jwtUtil,
                            PasswordEncoder passwordEncoder) {
        this.usuarioRepo     = usuarioRepo;
        this.jwtUtil         = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    /** GET /api/perfil — devuelve los datos del usuario autenticado */
    @GetMapping
    public ResponseEntity<PerfilDTO> obtener(@RequestHeader("Authorization") String authHeader) {
        Usuario u = resolverUsuario(authHeader);
        Persona p = u.getPersona();
        return ResponseEntity.ok(new PerfilDTO(
                p.getNombre(),
                p.getApellidoPaterno(),
                p.getApellidoMaterno(),
                p.getGenero(),
                p.getTelefono(),
                p.getFechaNacimiento() != null ? p.getFechaNacimiento().toString() : null,
                u.getEmail(),
                u.getRol().getNombre()
        ));
    }

    /** PUT /api/perfil — actualiza datos editables */
    @PutMapping
    public ResponseEntity<PerfilDTO> actualizar(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ActualizarPerfilRequestDTO dto) {

        Usuario u = resolverUsuario(authHeader);
        Persona p = u.getPersona();

        if (StringUtils.isNotBlank(dto.nombre()))
            p.setNombre(StringUtils.capitalize(dto.nombre().trim()));
        if (StringUtils.isNotBlank(dto.apellidoPaterno()))
            p.setApellidoPaterno(StringUtils.capitalize(dto.apellidoPaterno().trim()));
        p.setApellidoMaterno(StringUtils.isNotBlank(dto.apellidoMaterno())
                ? StringUtils.capitalize(dto.apellidoMaterno().trim()) : null);
        if (StringUtils.isNotBlank(dto.genero()))
            p.setGenero(dto.genero().trim().toUpperCase());
        p.setTelefono(StringUtils.trimToNull(dto.telefono()));
        if (StringUtils.isNotBlank(dto.fechaNacimiento()))
            p.setFechaNacimiento(java.time.LocalDate.parse(dto.fechaNacimiento()));

        // Cambio de contraseña — solo si envía ambos campos
        if (StringUtils.isNotBlank(dto.passwordActual()) && StringUtils.isNotBlank(dto.passwordNuevo())) {
            if (!passwordEncoder.matches(dto.passwordActual(), u.getPasswordHash()))
                throw new RuntimeException("La contraseña actual es incorrecta");
            if (dto.passwordNuevo().length() < 8)
                throw new RuntimeException("La nueva contraseña debe tener al menos 8 caracteres");
            u.setPasswordHash(passwordEncoder.encode(dto.passwordNuevo()));
        }

        usuarioRepo.save(u);

        return ResponseEntity.ok(new PerfilDTO(
                p.getNombre(), p.getApellidoPaterno(), p.getApellidoMaterno(),
                p.getGenero(), p.getTelefono(),
                p.getFechaNacimiento() != null ? p.getFechaNacimiento().toString() : null,
                u.getEmail(), u.getRol().getNombre()
        ));
    }

    private Usuario resolverUsuario(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.extraerEmail(token);
        return usuarioRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}
