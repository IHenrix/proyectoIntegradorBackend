package pe.edu.utp.pasajeya.app.service;

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
import pe.edu.utp.pasajeya.app.service.impl.AdminUsuarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepo;
    @Mock private RolRepository rolRepo;
    @Mock private PersonaRepository personaRepo;
    @Mock private TipoDocumentoRepository tipoDocRepo;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUsuarioServiceImpl adminUsuarioService;

    private Rol rolFree;
    private Rol rolPremium;
    private Usuario usuarioObjetivo;
    private Usuario usuarioAdminActual;

    @BeforeEach
    void setUp() {
        rolFree = new Rol();
        rolFree.setId(1);
        rolFree.setNombre("usuario_free");

        rolPremium = new Rol();
        rolPremium.setId(2);
        rolPremium.setNombre("usuario_premium");

        Persona persona = new Persona();
        persona.setNombre("Enrique");
        persona.setApellidoPaterno("Prada");

        usuarioObjetivo = new Usuario();
        usuarioObjetivo.setId(10);
        usuarioObjetivo.setEmail("enrique.pdg@gmail.com");
        usuarioObjetivo.setRol(rolFree);
        usuarioObjetivo.setActivo(true);
        usuarioObjetivo.setEmailVerificado(true);
        usuarioObjetivo.setPersona(persona);

        Persona personaAdmin = new Persona();
        personaAdmin.setNombre("Pedro");
        personaAdmin.setApellidoPaterno("Yarleque");

        usuarioAdminActual = new Usuario();
        usuarioAdminActual.setId(1);
        usuarioAdminActual.setEmail("admin@pasajeya.com.pe");
        Rol rolAdmin = new Rol();
        rolAdmin.setNombre("admin");
        usuarioAdminActual.setRol(rolAdmin);
        usuarioAdminActual.setActivo(true);
        usuarioAdminActual.setPersona(personaAdmin);
    }

    @Test
    @DisplayName("listar() pagina y mapea usuarios a DTO con nombre completo y rol")
    void listar_paginaYMapeaCorrectamente() {
        var pageable = PageRequest.of(0, 10);
        when(usuarioRepo.buscar(isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(usuarioObjetivo, usuarioAdminActual), pageable, 2));

        PaginaDTO<AdminUsuarioListadoDTO> resultado = adminUsuarioService.listar(0, 10, null);

        assertThat(resultado.contenido()).hasSize(2);
        assertThat(resultado.totalElementos()).isEqualTo(2);
        assertThat(resultado.contenido().get(0).nombreCompleto()).isEqualTo("Enrique Prada");
        assertThat(resultado.contenido().get(0).rol()).isEqualTo("usuario_free");
        assertThat(resultado.contenido().get(1).rol()).isEqualTo("admin");
    }

    @Test
    @DisplayName("listar() con búsqueda pasa el texto normalizado al repositorio")
    void listar_conBusqueda_pasaTextoAlRepo() {
        var pageable = PageRequest.of(0, 10);
        when(usuarioRepo.buscar(eq("enrique"), any()))
                .thenReturn(new PageImpl<>(List.of(usuarioObjetivo), pageable, 1));

        PaginaDTO<AdminUsuarioListadoDTO> resultado = adminUsuarioService.listar(0, 10, "enrique");

        assertThat(resultado.contenido()).hasSize(1);
        verify(usuarioRepo).buscar(eq("enrique"), any());
    }

    @Test
    @DisplayName("obtenerDetalle() devuelve el detalle completo del usuario")
    void obtenerDetalle_devuelveDatosCompletos() {
        when(usuarioRepo.findById(10)).thenReturn(Optional.of(usuarioObjetivo));

        AdminUsuarioDetalleDTO detalle = adminUsuarioService.obtenerDetalle(10);

        assertThat(detalle.email()).isEqualTo("enrique.pdg@gmail.com");
        assertThat(detalle.nombre()).isEqualTo("Enrique");
        assertThat(detalle.rol()).isEqualTo("usuario_free");
    }

    @Test
    @DisplayName("obtenerDetalle() con id inexistente lanza excepción clara")
    void obtenerDetalle_usuarioInexistente_lanzaExcepcion() {
        when(usuarioRepo.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUsuarioService.obtenerDetalle(999))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    @DisplayName("cambiarRol() actualiza el rol del usuario objetivo")
    void cambiarRol_actualizaRolCorrectamente() {
        when(usuarioRepo.findById(10)).thenReturn(Optional.of(usuarioObjetivo));
        when(rolRepo.findByNombre("usuario_premium")).thenReturn(Optional.of(rolPremium));
        when(usuarioRepo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUsuarioListadoDTO resultado = adminUsuarioService.cambiarRol(10, "usuario_premium", "admin@pasajeya.com.pe");

        assertThat(resultado.rol()).isEqualTo("usuario_premium");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepo).save(captor.capture());
        assertThat(captor.getValue().getRol().getNombre()).isEqualTo("usuario_premium");
    }

    @Test
    @DisplayName("cambiarRol() con rol inválido (no existe en BD) lanza excepción")
    void cambiarRol_rolInvalido_lanzaExcepcion() {
        when(usuarioRepo.findById(10)).thenReturn(Optional.of(usuarioObjetivo));
        when(rolRepo.findByNombre("super_admin")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUsuarioService.cambiarRol(10, "super_admin", "admin@pasajeya.com.pe"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Rol no válido");

        verify(usuarioRepo, never()).save(any());
    }

    @Test
    @DisplayName("cambiarRol() debe rechazar que un admin se cambie su propio rol (evita quedar sin admins)")
    void cambiarRol_autoAsignacion_debeRechazar() {
        when(usuarioRepo.findById(1)).thenReturn(Optional.of(usuarioAdminActual));

        assertThatThrownBy(() -> adminUsuarioService.cambiarRol(1, "usuario_free", "admin@pasajeya.com.pe"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No puedes cambiar tu propio rol");

        verify(usuarioRepo, never()).save(any());
    }

    @Test
    @DisplayName("cambiarEstadoActivo() desactiva un usuario distinto sin problema")
    void cambiarEstadoActivo_desactivaOtroUsuario() {
        when(usuarioRepo.findById(10)).thenReturn(Optional.of(usuarioObjetivo));
        when(usuarioRepo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUsuarioListadoDTO resultado = adminUsuarioService.cambiarEstadoActivo(10, false, "admin@pasajeya.com.pe");

        assertThat(resultado.activo()).isFalse();
    }

    @Test
    @DisplayName("cambiarEstadoActivo() debe rechazar que un admin se auto-desactive")
    void cambiarEstadoActivo_autoDesactivacion_debeRechazar() {
        when(usuarioRepo.findById(1)).thenReturn(Optional.of(usuarioAdminActual));

        assertThatThrownBy(() -> adminUsuarioService.cambiarEstadoActivo(1, false, "admin@pasajeya.com.pe"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No puedes desactivar tu propia cuenta");

        verify(usuarioRepo, never()).save(any());
    }

    @Test
    @DisplayName("cambiarEstadoActivo() SÍ permite que un admin se auto-reactive (solo se bloquea desactivar)")
    void cambiarEstadoActivo_autoReactivacion_siPermitido() {
        usuarioAdminActual.setActivo(false);
        when(usuarioRepo.findById(1)).thenReturn(Optional.of(usuarioAdminActual));
        when(usuarioRepo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUsuarioListadoDTO resultado = adminUsuarioService.cambiarEstadoActivo(1, true, "admin@pasajeya.com.pe");

        assertThat(resultado.activo()).isTrue();
    }

    // ── crear() ──────────────────────────────────────────────────────────

    private CrearUsuarioRequestDTO crearDtoValido() {
        return new CrearUsuarioRequestDTO(
                "Ana", "Torres", null, null,
                "ana.nueva@test.com", "password123",
                null, null, null, null,
                "usuario_free");
    }

    @Test
    @DisplayName("crear() guarda un usuario nuevo con los datos normalizados y rol elegido por el admin")
    void crear_guardaUsuarioNuevo() {
        when(usuarioRepo.existsByEmail("ana.nueva@test.com")).thenReturn(false);
        when(rolRepo.findByNombre("usuario_free")).thenReturn(Optional.of(rolFree));
        when(passwordEncoder.encode("password123")).thenReturn("hash-simulado");
        when(usuarioRepo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUsuarioListadoDTO resultado = adminUsuarioService.crear(crearDtoValido());

        assertThat(resultado.email()).isEqualTo("ana.nueva@test.com");
        assertThat(resultado.rol()).isEqualTo("usuario_free");
        assertThat(resultado.activo()).isTrue();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepo).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hash-simulado");
        assertThat(captor.getValue().getEmailVerificado()).isTrue();
    }

    @Test
    @DisplayName("crear() con email ya registrado lanza excepción y no guarda nada")
    void crear_emailDuplicado_lanzaExcepcion() {
        when(usuarioRepo.existsByEmail("ana.nueva@test.com")).thenReturn(true);

        assertThatThrownBy(() -> adminUsuarioService.crear(crearDtoValido()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("correo ya está registrado");

        verify(usuarioRepo, never()).save(any());
    }

    @Test
    @DisplayName("crear() con rol inválido lanza excepción y no guarda nada")
    void crear_rolInvalido_lanzaExcepcion() {
        when(usuarioRepo.existsByEmail("ana.nueva@test.com")).thenReturn(false);
        when(rolRepo.findByNombre("usuario_free")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUsuarioService.crear(crearDtoValido()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Rol no válido");

        verify(usuarioRepo, never()).save(any());
    }

    // ── editar() ─────────────────────────────────────────────────────────

    private EditarUsuarioRequestDTO editarDtoSinPassword() {
        return new EditarUsuarioRequestDTO(
                "Enrique", "Prada", null, null,
                "enrique.pdg@gmail.com", "",
                null, null, null, null,
                "usuario_premium", true);
    }

    @Test
    @DisplayName("editar() sin password no cambia el hash existente")
    void editar_sinPassword_noCambiaHash() {
        usuarioObjetivo.setPasswordHash("hash-original");
        when(usuarioRepo.findById(10)).thenReturn(Optional.of(usuarioObjetivo));
        when(usuarioRepo.existsByEmailAndIdNot(anyString(), any())).thenReturn(false);
        when(rolRepo.findByNombre("usuario_premium")).thenReturn(Optional.of(rolPremium));
        when(usuarioRepo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        adminUsuarioService.editar(10, editarDtoSinPassword());

        assertThat(usuarioObjetivo.getPasswordHash()).isEqualTo("hash-original");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("editar() con password corta (menos de 8) lanza excepción")
    void editar_passwordCorta_lanzaExcepcion() {
        when(usuarioRepo.findById(10)).thenReturn(Optional.of(usuarioObjetivo));
        when(usuarioRepo.existsByEmailAndIdNot(anyString(), any())).thenReturn(false);

        EditarUsuarioRequestDTO dto = new EditarUsuarioRequestDTO(
                "Enrique", "Prada", null, null,
                "enrique.pdg@gmail.com", "corta",
                null, null, null, null,
                "usuario_free", true);

        assertThatThrownBy(() -> adminUsuarioService.editar(10, dto))
                .isInstanceOf(IllegalArgumentException.class);

        verify(usuarioRepo, never()).save(any());
    }

    @Test
    @DisplayName("editar() cambia rol y estado activo correctamente")
    void editar_cambiaRolYActivo() {
        when(usuarioRepo.findById(10)).thenReturn(Optional.of(usuarioObjetivo));
        when(usuarioRepo.existsByEmailAndIdNot(anyString(), any())).thenReturn(false);
        when(rolRepo.findByNombre("usuario_premium")).thenReturn(Optional.of(rolPremium));
        when(usuarioRepo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        EditarUsuarioRequestDTO dto = new EditarUsuarioRequestDTO(
                "Enrique", "Prada", null, null,
                "enrique.pdg@gmail.com", null,
                null, null, null, null,
                "usuario_premium", false);

        AdminUsuarioListadoDTO resultado = adminUsuarioService.editar(10, dto);

        assertThat(resultado.rol()).isEqualTo("usuario_premium");
        assertThat(resultado.activo()).isFalse();
    }

    @Test
    @DisplayName("editar() con email ya usado por otro usuario lanza excepción")
    void editar_emailDuplicado_lanzaExcepcion() {
        when(usuarioRepo.findById(10)).thenReturn(Optional.of(usuarioObjetivo));
        when(usuarioRepo.existsByEmailAndIdNot("otro@test.com", 10)).thenReturn(true);

        EditarUsuarioRequestDTO dto = new EditarUsuarioRequestDTO(
                "Enrique", "Prada", null, null,
                "otro@test.com", null,
                null, null, null, null,
                "usuario_free", true);

        assertThatThrownBy(() -> adminUsuarioService.editar(10, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("correo ya está registrado");

        verify(usuarioRepo, never()).save(any());
    }
}
