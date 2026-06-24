package pe.edu.utp.pasajeya.app.repository;

import pe.edu.utp.pasajeya.app.model.Aeropuerto;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class AeropuertoRepositoryTest {

    @Autowired
    private AeropuertoRepository aeropuertoRepository;

    @BeforeEach
    void setUp() {
        aeropuertoRepository.deleteAll();
        aeropuertoRepository.save(crearAeropuerto("LIM", "Jorge Chavez", "Lima", "PE"));
        aeropuertoRepository.save(crearAeropuerto("CUZ", "Alejandro Velasco Astete", "Cusco", "PE"));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 1: Guardar y encontrar por codigo (clave primaria)
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe guardar un aeropuerto y encontrarlo por codigo")
    void debeGuardarYEncontrarPorCodigo() {
        Optional<Aeropuerto> encontrado = aeropuertoRepository.findById("LIM");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getCiudad()).isEqualTo("Lima");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 2: Listar todos los aeropuertos guardados
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe listar todos los aeropuertos guardados")
    void debeListarTodosLosAeropuertos() {
        List<Aeropuerto> todos = aeropuertoRepository.findAll();

        assertThat(todos).hasSize(2);
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 3: Eliminar un aeropuerto
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe eliminar el aeropuerto correctamente")
    void debeEliminarAeropuerto() {
        aeropuertoRepository.deleteById("CUZ");

        Optional<Aeropuerto> eliminado = aeropuertoRepository.findById("CUZ");
        assertThat(eliminado).isEmpty();
    }

    private Aeropuerto crearAeropuerto(String codigo, String nombre, String ciudad, String pais) {
        Aeropuerto a = new Aeropuerto();
        a.setCodigo(codigo);
        a.setNombre(nombre);
        a.setCiudad(ciudad);
        a.setPais(pais);
        return a;
    }
}
