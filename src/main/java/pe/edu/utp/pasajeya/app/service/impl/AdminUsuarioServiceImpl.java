package pe.edu.utp.pasajeya.app.service.impl;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import pe.edu.utp.pasajeya.app.dto.AdminUsuarioDetalleDTO;
import pe.edu.utp.pasajeya.app.dto.AdminUsuarioListadoDTO;
import pe.edu.utp.pasajeya.app.dto.CrearUsuarioRequestDTO;
import pe.edu.utp.pasajeya.app.dto.EditarUsuarioRequestDTO;
import pe.edu.utp.pasajeya.app.dto.PaginaDTO;
import pe.edu.utp.pasajeya.app.model.Persona;
import pe.edu.utp.pasajeya.app.model.Rol;
import pe.edu.utp.pasajeya.app.model.Usuario;
import pe.edu.utp.pasajeya.app.repository.PersonaRepository;
import pe.edu.utp.pasajeya.app.repository.RolRepository;
import pe.edu.utp.pasajeya.app.repository.TipoDocumentoRepository;
import pe.edu.utp.pasajeya.app.repository.UsuarioRepository;
import pe.edu.utp.pasajeya.app.service.AdminUsuarioService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AdminUsuarioServiceImpl implements AdminUsuarioService {

    private final UsuarioRepository usuarioRepo;
    private final RolRepository rolRepo;
    private final PersonaRepository personaRepo;
    private final TipoDocumentoRepository tipoDocRepo;
    private final PasswordEncoder passwordEncoder;

    public AdminUsuarioServiceImpl(UsuarioRepository usuarioRepo,
                                    RolRepository rolRepo,
                                    PersonaRepository personaRepo,
                                    TipoDocumentoRepository tipoDocRepo,
                                    PasswordEncoder passwordEncoder) {
        this.usuarioRepo = usuarioRepo;
        this.rolRepo = rolRepo;
        this.personaRepo = personaRepo;
        this.tipoDocRepo = tipoDocRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaDTO<AdminUsuarioListadoDTO> listar(int pagina, int tamano, String busqueda) {
        var pageable = PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "id"));
        String q = StringUtils.trimToNull(busqueda);
        var page = usuarioRepo.buscar(q, pageable).map(this::toListadoDto);
        return PaginaDTO.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<AdminUsuarioListadoDTO> listarTodos() {
        return usuarioRepo.findAll().stream().map(this::toListadoDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUsuarioDetalleDTO obtenerDetalle(Integer idUsuario) {
        Usuario u = usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return toDetalleDto(u);
    }

    @Override
    @Transactional
    public AdminUsuarioListadoDTO cambiarRol(Integer idUsuario, String nuevoRol, String emailAdminActual) {
        Usuario u = usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (u.getEmail().equalsIgnoreCase(emailAdminActual)) {
            throw new RuntimeException("No puedes cambiar tu propio rol");
        }

        Rol rol = rolRepo.findByNombre(nuevoRol)
                .orElseThrow(() -> new RuntimeException("Rol no válido: " + nuevoRol));

        u.setRol(rol);
        return toListadoDto(usuarioRepo.save(u));
    }

    @Override
    @Transactional
    public AdminUsuarioListadoDTO cambiarEstadoActivo(Integer idUsuario, boolean activo, String emailAdminActual) {
        Usuario u = usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!activo && u.getEmail().equalsIgnoreCase(emailAdminActual)) {
            throw new RuntimeException("No puedes desactivar tu propia cuenta");
        }

        u.setActivo(activo);
        return toListadoDto(usuarioRepo.save(u));
    }

    @Override
    @Transactional
    public AdminUsuarioListadoDTO crear(CrearUsuarioRequestDTO dto) {
        String emailNormalizado = StringUtils.lowerCase(StringUtils.trimToEmpty(dto.email()));
        if (usuarioRepo.existsByEmail(emailNormalizado)) {
            throw new RuntimeException("El correo ya está registrado");
        }

        String nroDoc = StringUtils.trimToNull(dto.nroDocumento());
        if (nroDoc != null && personaRepo.existsByNroDocumento(nroDoc)) {
            throw new RuntimeException("El número de documento ya está registrado");
        }

        Rol rol = rolRepo.findByNombre(dto.rol())
                .orElseThrow(() -> new RuntimeException("Rol no válido: " + dto.rol()));

        Persona persona = new Persona();
        aplicarDatosPersona(persona, dto.nombre(), dto.apellidoPaterno(), dto.apellidoMaterno(),
                dto.genero(), dto.telefono(), dto.fechaNacimiento(), dto.tipoDocumentoId(), nroDoc);
        persona.setFechaRegistro(LocalDateTime.now());

        Usuario usuario = new Usuario();
        usuario.setPersona(persona);
        usuario.setRol(rol);
        usuario.setEmail(emailNormalizado);
        usuario.setPasswordHash(passwordEncoder.encode(dto.password()));
        usuario.setProveedor("email");
        usuario.setActivo(true);
        // Verificado desde ya: es el propio admin quien da de alta la cuenta,
        // no tiene sentido exigirle un flujo de verificación por correo.
        usuario.setEmailVerificado(true);

        return toListadoDto(usuarioRepo.save(usuario));
    }

    @Override
    @Transactional
    public AdminUsuarioListadoDTO editar(Integer idUsuario, EditarUsuarioRequestDTO dto) {
        Usuario u = usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String emailNormalizado = StringUtils.lowerCase(StringUtils.trimToEmpty(dto.email()));
        if (usuarioRepo.existsByEmailAndIdNot(emailNormalizado, idUsuario)) {
            throw new RuntimeException("El correo ya está registrado por otro usuario");
        }

        String nroDoc = StringUtils.trimToNull(dto.nroDocumento());
        if (nroDoc != null && personaRepo.existsByNroDocumentoAndIdNot(nroDoc,
                u.getPersona() != null ? u.getPersona().getId() : -1)) {
            throw new RuntimeException("El número de documento ya está registrado por otro usuario");
        }

        Persona persona = u.getPersona();
        if (persona == null) {
            persona = new Persona();
            persona.setFechaRegistro(LocalDateTime.now());
            u.setPersona(persona);
        }
        aplicarDatosPersona(persona, dto.nombre(), dto.apellidoPaterno(), dto.apellidoMaterno(),
                dto.genero(), dto.telefono(), dto.fechaNacimiento(), dto.tipoDocumentoId(), nroDoc);

        u.setEmail(emailNormalizado);

        if (StringUtils.isNotBlank(dto.password())) {
            Preconditions.checkArgument(dto.password().length() >= 8,
                    "La contraseña debe tener al menos 8 caracteres");
            u.setPasswordHash(passwordEncoder.encode(dto.password()));
        }

        Rol rol = rolRepo.findByNombre(dto.rol())
                .orElseThrow(() -> new RuntimeException("Rol no válido: " + dto.rol()));
        u.setRol(rol);

        if (dto.activo() != null) {
            u.setActivo(dto.activo());
        }

        return toListadoDto(usuarioRepo.save(u));
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private void aplicarDatosPersona(Persona persona, String nombre, String apellidoPaterno,
                                      String apellidoMaterno, String genero, String telefono,
                                      String fechaNacimiento, Integer tipoDocumentoId, String nroDoc) {
        persona.setNombre(StringUtils.capitalize(StringUtils.trimToEmpty(nombre)));
        persona.setApellidoPaterno(StringUtils.capitalize(StringUtils.trimToEmpty(apellidoPaterno)));
        persona.setApellidoMaterno(StringUtils.isNotBlank(apellidoMaterno)
                ? StringUtils.capitalize(StringUtils.trimToEmpty(apellidoMaterno)) : null);
        if (StringUtils.isNotBlank(genero)) {
            persona.setGenero(genero.trim().toUpperCase());
        }
        persona.setTelefono(StringUtils.trimToEmpty(telefono));
        if (StringUtils.isNotBlank(fechaNacimiento)) {
            persona.setFechaNacimiento(LocalDate.parse(fechaNacimiento));
        }
        if (tipoDocumentoId != null) {
            tipoDocRepo.findById(tipoDocumentoId).ifPresent(persona::setTipoDocumento);
        }
        if (nroDoc != null) {
            persona.setNroDocumento(nroDoc);
        }
    }

    private AdminUsuarioListadoDTO toListadoDto(Usuario u) {
        Persona p = u.getPersona();
        String nombreCompleto = p != null
                ? (p.getNombre() + " " + p.getApellidoPaterno()).trim()
                : "—";
        return new AdminUsuarioListadoDTO(
                u.getId(),
                u.getEmail(),
                nombreCompleto,
                u.getRol().getNombre(),
                u.getActivo(),
                u.getEmailVerificado(),
                p != null && p.getFechaRegistro() != null ? p.getFechaRegistro().toString() : null
        );
    }

    private AdminUsuarioDetalleDTO toDetalleDto(Usuario u) {
        Persona p = u.getPersona();
        return new AdminUsuarioDetalleDTO(
                u.getId(),
                u.getEmail(),
                p != null ? p.getNombre() : null,
                p != null ? p.getApellidoPaterno() : null,
                p != null ? p.getApellidoMaterno() : null,
                p != null ? p.getGenero() : null,
                p != null ? p.getTelefono() : null,
                p != null && p.getFechaNacimiento() != null ? p.getFechaNacimiento().toString() : null,
                p != null && p.getTipoDocumento() != null ? p.getTipoDocumento().getCodigo() : null,
                p != null ? p.getNroDocumento() : null,
                u.getRol().getNombre(),
                u.getActivo(),
                u.getEmailVerificado(),
                p != null && p.getFechaRegistro() != null ? p.getFechaRegistro().toString() : null
        );
    }
}
