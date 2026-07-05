package pe.edu.utp.pasajeya.app.service.impl;

import pe.edu.utp.pasajeya.app.dto.AdminUsuarioDetalleDTO;
import pe.edu.utp.pasajeya.app.dto.AdminUsuarioListadoDTO;
import pe.edu.utp.pasajeya.app.model.Persona;
import pe.edu.utp.pasajeya.app.model.Rol;
import pe.edu.utp.pasajeya.app.model.Usuario;
import pe.edu.utp.pasajeya.app.repository.RolRepository;
import pe.edu.utp.pasajeya.app.repository.UsuarioRepository;
import pe.edu.utp.pasajeya.app.service.AdminUsuarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminUsuarioServiceImpl implements AdminUsuarioService {

    private final UsuarioRepository usuarioRepo;
    private final RolRepository rolRepo;

    public AdminUsuarioServiceImpl(UsuarioRepository usuarioRepo, RolRepository rolRepo) {
        this.usuarioRepo = usuarioRepo;
        this.rolRepo = rolRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUsuarioListadoDTO> listar() {
        return usuarioRepo.findAll().stream()
                .map(this::toListadoDto)
                .toList();
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

    // ── Helpers ──────────────────────────────────────────────────────────

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
