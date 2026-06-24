package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.AeropuertoDTO;
import pe.edu.utp.pasajeya.app.model.Aeropuerto;
import pe.edu.utp.pasajeya.app.repository.AeropuertoRepository;
import pe.edu.utp.pasajeya.app.service.impl.AeropuertoServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AeropuertoServiceTest {

    @Mock
    private AeropuertoRepository repo;

    @InjectMocks
    private AeropuertoServiceImpl aeropuertoService;

    // ═══════════════════════════════════════════════════
    // PRUEBA 1: Lista vacia
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe retornar lista vacia cuando no hay aeropuertos")
    void cuandoNoHayAeropuertos_debeRetornarListaVacia() {
        when(repo.findAll()).thenReturn(List.of());

        List<AeropuertoDTO> resultado = aeropuertoService.listar();

        assertThat(resultado).isEmpty();
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 2: Mapea pais PE a "Peru" y ordena por ciudad
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe mapear el pais PE a Peru y ordenar por ciudad")
    void cuandoListaAeropuertos_debeMapearPaisYOrdenarPorCiudad() {
        Aeropuerto cuz = crearAeropuerto("CUZ", "Aeropuerto Alejandro Velasco Astete", "Cusco", "PE");
        Aeropuerto lim = crearAeropuerto("LIM", "Jorge Chavez", "Lima", "PE");
        when(repo.findAll()).thenReturn(List.of(cuz, lim));

        List<AeropuertoDTO> resultado = aeropuertoService.listar();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).ciudad()).isEqualTo("Cusco");
        assertThat(resultado.get(1).ciudad()).isEqualTo("Lima");
        assertThat(resultado.get(0).pais()).isEqualTo("Perú");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 3: Pais nulo o desconocido cae a "Peru" por defecto
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe usar Peru por defecto cuando el pais es nulo")
    void cuandoPaisEsNulo_debeUsarPeruPorDefecto() {
        Aeropuerto sinPais = crearAeropuerto("AQP", "Rodriguez Ballon", "Arequipa", null);
        when(repo.findAll()).thenReturn(List.of(sinPais));

        List<AeropuertoDTO> resultado = aeropuertoService.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).pais()).isEqualTo("Perú");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 4: El codigo se retorna sin espacios (trim)
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe quitar espacios del codigo del aeropuerto")
    void cuandoCodigoTieneEspacios_debeRetornarCodigoSinEspacios() {
        Aeropuerto conEspacios = crearAeropuerto(" LIM ", "Jorge Chavez", "Lima", "PE");
        when(repo.findAll()).thenReturn(List.of(conEspacios));

        List<AeropuertoDTO> resultado = aeropuertoService.listar();

        assertThat(resultado.get(0).code()).isEqualTo("LIM");
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
