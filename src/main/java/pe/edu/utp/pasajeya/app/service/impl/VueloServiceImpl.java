package pe.edu.utp.pasajeya.app.service.impl;

import com.google.common.collect.ImmutableList;
import pe.edu.utp.pasajeya.app.dto.VueloDTO;
import pe.edu.utp.pasajeya.app.model.Tarifa;
import pe.edu.utp.pasajeya.app.model.Vuelo;
import pe.edu.utp.pasajeya.app.repository.HistorialPrecioRepository;
import pe.edu.utp.pasajeya.app.repository.TarifaRepository;
import pe.edu.utp.pasajeya.app.repository.VueloRepository;
import pe.edu.utp.pasajeya.app.service.VueloService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class VueloServiceImpl implements VueloService {

    private static final Logger log = LoggerFactory.getLogger(VueloServiceImpl.class);

    private final VueloRepository          vueloRepo;
    private final TarifaRepository         tarifaRepo;
    private final HistorialPrecioRepository historialRepo;

    public VueloServiceImpl(VueloRepository vueloRepo,
                            TarifaRepository tarifaRepo,
                            HistorialPrecioRepository historialRepo) {
        this.vueloRepo    = vueloRepo;
        this.tarifaRepo   = tarifaRepo;
        this.historialRepo = historialRepo;
    }

    @Override
    public List<VueloDTO> buscarVuelos(String origen, String destino, String fecha, int pasajeros) {
        log.info("Buscando vuelos: {} → {} ({}), pasajeros: {}", origen, destino, fecha, pasajeros);

        LocalDate fechaSalida = LocalDate.parse(fecha);
        List<Vuelo> vuelos = vueloRepo.findByOrigenAndDestinoAndFechaSalida(
                origen.trim(), destino.trim(), fechaSalida);

        log.info("Vuelos encontrados en BD: {}", vuelos.size());

        LocalDateTime treintaDiasAtras = LocalDateTime.now().minusDays(30);
        List<VueloDTO> resultado = new ArrayList<>();

        for (Vuelo vuelo : vuelos) {
            // Google Guava — ImmutableList para proteger la lista de tarifas durante iteracion
            List<Tarifa> tarifas = ImmutableList.copyOf(
                    tarifaRepo.findByVueloOrderByPrecioAsc(vuelo));

            for (Tarifa tarifa : tarifas) {
                LocalTime horaLlegada = vuelo.getHoraSalida().plusMinutes(vuelo.getDuracionMin());
                String duracion = formatearDuracion(vuelo.getDuracionMin());
                String semaforo = calcularSemaforo(vuelo.getId(), tarifa.getTipo(),
                        tarifa.getPrecio().doubleValue(), treintaDiasAtras);
                String aerolinea    = nombreCorto(vuelo.getAerolinea().getNombre());
                String urlAerolinea = vuelo.getAerolinea().getUrlWeb() != null
                        ? vuelo.getAerolinea().getUrlWeb() : "#";

                resultado.add(new VueloDTO(
                        tarifa.getId().longValue(),
                        aerolinea,
                        vuelo.getOrigen().trim(),
                        vuelo.getDestino().trim(),
                        vuelo.getFechaSalida().toString(),
                        formatearHora(vuelo.getHoraSalida()),
                        formatearHora(horaLlegada),
                        duracion,
                        tarifa.getPrecio().doubleValue(),
                        tarifa.getTipo(),
                        tarifa.getEquipajeBodegaKg() > 0,
                        tarifa.getEquipajeBodegaKg(),
                        tarifa.getEquipajeManoKg(),
                        tarifa.getPermiteReembolso(),
                        tarifa.getAsientoSeleccionable(),
                        semaforo,
                        urlAerolinea
                ));
            }
        }

        resultado.sort((a, b) -> Double.compare(a.precio(), b.precio()));
        log.info("Total tarifas a devolver: {}", resultado.size());
        return resultado;
    }

    private String calcularSemaforo(Integer idVuelo, String tipoTarifa,
                                    double precioActual, LocalDateTime desde) {
        Optional<Double> promedio = historialRepo.calcularPromedio(idVuelo, tipoTarifa, desde);
        if (promedio.isEmpty()) {
            return "amarillo";
        }
        double avg = promedio.get();
        if (precioActual < avg * 0.95) return "verde";
        if (precioActual > avg * 1.05) return "rojo";
        return "amarillo";
    }

    private String nombreCorto(String nombre) {
        String upper = nombre.toUpperCase();
        if (upper.contains("LATAM"))    return "LATAM";
        if (upper.contains("SKY"))      return "Sky";
        if (upper.contains("JETSMART")) return "JetSmart";
        return nombre;
    }

    private String formatearHora(LocalTime hora) {
        return String.format("%02d:%02d", hora.getHour(), hora.getMinute());
    }

    private String formatearDuracion(int minutos) {
        int horas = minutos / 60;
        int mins  = minutos % 60;
        return horas > 0
                ? String.format("%dh %02dm", horas, mins)
                : String.format("%dm", mins);
    }
}
