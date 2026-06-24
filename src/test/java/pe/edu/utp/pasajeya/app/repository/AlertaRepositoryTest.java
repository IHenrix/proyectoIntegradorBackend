package pe.edu.utp.pasajeya.app.repository;

import pe.edu.utp.pasajeya.app.model.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class AlertaRepositoryTest {

    @Autowired
    private AlertaRepository alertaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RolRepository rolRepository;
    @Autowired
    private VueloRepository vueloRepository;
    @Autowired
    private TestEntityManager entityManager;

    private Usuario usuarioAna;
    private Usuario usuarioLuis;
    private Vuelo vuelo;

    @BeforeEach
    void setUp() {
        alertaRepository.deleteAll();
        usuarioRepository.deleteAll();

        Rol rolFree = rolRepository.save(crearRol("usuario_free"));

        usuarioAna = usuarioRepository.save(crearUsuario("ana@test.com", rolFree));
        usuarioLuis = usuarioRepository.save(crearUsuario("luis@test.com", rolFree));

        Aerolinea aerolinea = entityManager.persistAndFlush(crearAerolinea("LATAM Airlines"));
        vuelo = vueloRepository.save(crearVuelo(aerolinea));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 1: Guardar y recuperar alerta por ID
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe guardar una alerta y encontrarla por ID")
    void debeGuardarYEncontrarPorId() {
        Alerta alerta = alertaRepository.save(crearAlerta(usuarioAna, vuelo, "basica", 200.0));

        Optional<Alerta> encontrada = alertaRepository.findById(alerta.getId());

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getTipoTarifa()).isEqualTo("basica");
        assertThat(encontrada.get().getPrecioObjetivo()).isEqualTo(BigDecimal.valueOf(200.0));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 2: Contar alertas por usuario
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe contar correctamente las alertas de un usuario")
    void debeContarAlertasPorUsuario() {
        alertaRepository.save(crearAlerta(usuarioAna, vuelo, "basica", 200.0));
        alertaRepository.save(crearAlerta(usuarioAna, vuelo, "flex", 300.0));
        alertaRepository.save(crearAlerta(usuarioLuis, vuelo, "basica", 150.0));

        long totalAna = alertaRepository.countByUsuarioEmail("ana@test.com");

        assertThat(totalAna).isEqualTo(2);
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 3: Listar todas las alertas guardadas
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe listar todas las alertas de un usuario ordenadas por fecha")
    void debeListarTodasLasAlertas() {
        alertaRepository.save(crearAlerta(usuarioAna, vuelo, "basica", 200.0));
        alertaRepository.save(crearAlerta(usuarioAna, vuelo, "flex", 300.0));

        List<Alerta> alertas = alertaRepository.findByUsuarioEmailOrderByFechaCreacionDesc("ana@test.com");

        assertThat(alertas).hasSize(2);
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 4: Eliminar alerta
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe eliminar la alerta correctamente")
    void debeEliminarAlerta() {
        Alerta alerta = alertaRepository.save(crearAlerta(usuarioAna, vuelo, "basica", 200.0));

        alertaRepository.delete(alerta);

        Optional<Alerta> eliminada = alertaRepository.findById(alerta.getId());
        assertThat(eliminada).isEmpty();
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 5: Detectar alerta duplicada
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe detectar que ya existe una alerta para el mismo usuario, vuelo y tipo de tarifa")
    void debeDetectarAlertaDuplicada() {
        alertaRepository.save(crearAlerta(usuarioAna, vuelo, "basica", 200.0));

        boolean existeDuplicada = alertaRepository.existsByUsuarioEmailAndVueloIdAndTipoTarifa(
                "ana@test.com", vuelo.getId(), "basica");
        boolean noExisteParaOtroTipo = alertaRepository.existsByUsuarioEmailAndVueloIdAndTipoTarifa(
                "ana@test.com", vuelo.getId(), "flex");

        assertThat(existeDuplicada).isTrue();
        assertThat(noExisteParaOtroTipo).isFalse();
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 6 (extra): findByActivaTrue filtra solo activas
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe retornar solo las alertas activas")
    void debeRetornarSoloAlertasActivas() {
        Alerta activa = alertaRepository.save(crearAlerta(usuarioAna, vuelo, "basica", 200.0));
        Alerta pausada = crearAlerta(usuarioLuis, vuelo, "flex", 300.0);
        pausada.setActiva(false);
        alertaRepository.save(pausada);

        List<Alerta> activas = alertaRepository.findByActivaTrue();

        assertThat(activas).hasSize(1);
        assertThat(activas.get(0).getId()).isEqualTo(activa.getId());
    }

    private Rol crearRol(String nombre) {
        Rol rol = new Rol();
        rol.setNombre(nombre);
        rol.setDescripcion(nombre);
        return rol;
    }

    private Usuario crearUsuario(String email, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPasswordHash("hash");
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        return usuario;
    }

    private Aerolinea crearAerolinea(String nombre) {
        Aerolinea aerolinea = new Aerolinea();
        aerolinea.setNombre(nombre);
        aerolinea.setCodigo("LA");
        return aerolinea;
    }

    private Vuelo crearVuelo(Aerolinea aerolinea) {
        Vuelo vuelo = new Vuelo();
        vuelo.setAerolinea(aerolinea);
        vuelo.setOrigen("LIM");
        vuelo.setDestino("CUZ");
        vuelo.setFechaSalida(LocalDate.of(2026, 7, 1));
        vuelo.setHoraSalida(LocalTime.of(8, 30));
        vuelo.setDuracionMin(80);
        return vuelo;
    }

    private Alerta crearAlerta(Usuario usuario, Vuelo vuelo, String tipoTarifa, double precioObjetivo) {
        Alerta alerta = new Alerta();
        alerta.setUsuario(usuario);
        alerta.setVuelo(vuelo);
        alerta.setTipoTarifa(tipoTarifa);
        alerta.setPrecioObjetivo(BigDecimal.valueOf(precioObjetivo));
        alerta.setTelefono("987654321");
        alerta.setActiva(true);
        alerta.setFechaCreacion(LocalDateTime.now());
        return alerta;
    }
}
