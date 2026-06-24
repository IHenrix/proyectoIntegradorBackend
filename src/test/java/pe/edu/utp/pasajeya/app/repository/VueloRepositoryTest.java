package pe.edu.utp.pasajeya.app.repository;

import pe.edu.utp.pasajeya.app.model.Aerolinea;
import pe.edu.utp.pasajeya.app.model.Tarifa;
import pe.edu.utp.pasajeya.app.model.Vuelo;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class VueloRepositoryTest {

    @Autowired
    private VueloRepository vueloRepository;
    @Autowired
    private TarifaRepository tarifaRepository;
    @Autowired
    private TestEntityManager entityManager;

    private Vuelo vueloLimCuz;

    @BeforeEach
    void setUp() {
        tarifaRepository.deleteAll();
        vueloRepository.deleteAll();

        Aerolinea aerolinea = entityManager.persistAndFlush(crearAerolinea("LATAM Airlines"));

        vueloLimCuz = vueloRepository.save(crearVuelo(aerolinea, "LIM", "CUZ", LocalDate.of(2026, 7, 1)));
        vueloRepository.save(crearVuelo(aerolinea, "LIM", "AQP", LocalDate.of(2026, 7, 1)));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 1: Buscar por origen, destino y fecha exactos
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe encontrar vuelos por origen, destino y fecha de salida")
    void debeEncontrarVuelosPorOrigenDestinoYFecha() {
        List<Vuelo> resultado = vueloRepository.findByOrigenAndDestinoAndFechaSalida(
                "LIM", "CUZ", LocalDate.of(2026, 7, 1));

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getDestino()).isEqualTo("CUZ");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 2: No debe encontrar vuelos para una ruta inexistente
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe retornar lista vacia cuando la ruta no existe")
    void debeRetornarVacioParaRutaInexistente() {
        List<Vuelo> resultado = vueloRepository.findByOrigenAndDestinoAndFechaSalida(
                "LIM", "TPP", LocalDate.of(2026, 7, 1));

        assertThat(resultado).isEmpty();
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 3: Tarifas ordenadas por precio ascendente para un vuelo
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe retornar las tarifas de un vuelo ordenadas por precio ascendente")
    void debeOrdenarTarifasPorPrecioAscendente() {
        tarifaRepository.save(crearTarifa(vueloLimCuz, "flex", 400.0));
        tarifaRepository.save(crearTarifa(vueloLimCuz, "basica", 250.0));

        List<Tarifa> tarifas = tarifaRepository.findByVueloOrderByPrecioAsc(vueloLimCuz);

        assertThat(tarifas).hasSize(2);
        assertThat(tarifas.get(0).getPrecio()).isEqualTo(BigDecimal.valueOf(250.0));
        assertThat(tarifas.get(1).getPrecio()).isEqualTo(BigDecimal.valueOf(400.0));
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 4: Encuentra la tarifa mas barata de un tipo especifico
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe encontrar la tarifa mas barata de un tipo especifico para el vuelo")
    void debeEncontrarTarifaMasBarataPorTipo() {
        tarifaRepository.save(crearTarifa(vueloLimCuz, "basica", 280.0));
        tarifaRepository.save(crearTarifa(vueloLimCuz, "basica", 250.0));

        Optional<Tarifa> masBarata = tarifaRepository.findFirstByVueloAndTipoOrderByPrecioAsc(vueloLimCuz, "basica");

        assertThat(masBarata).isPresent();
        assertThat(masBarata.get().getPrecio()).isEqualTo(BigDecimal.valueOf(250.0));
    }

    private Aerolinea crearAerolinea(String nombre) {
        Aerolinea a = new Aerolinea();
        a.setNombre(nombre);
        a.setCodigo("LA");
        return a;
    }

    private Vuelo crearVuelo(Aerolinea aerolinea, String origen, String destino, LocalDate fecha) {
        Vuelo v = new Vuelo();
        v.setAerolinea(aerolinea);
        v.setOrigen(origen);
        v.setDestino(destino);
        v.setFechaSalida(fecha);
        v.setHoraSalida(LocalTime.of(8, 30));
        v.setDuracionMin(80);
        return v;
    }

    private Tarifa crearTarifa(Vuelo vuelo, String tipo, double precio) {
        Tarifa t = new Tarifa();
        t.setVuelo(vuelo);
        t.setTipo(tipo);
        t.setPrecio(BigDecimal.valueOf(precio));
        t.setEquipajeBodegaKg(0);
        t.setEquipajeManoKg(8);
        t.setPermiteReembolso(false);
        t.setAsientoSeleccionable(false);
        return t;
    }
}
