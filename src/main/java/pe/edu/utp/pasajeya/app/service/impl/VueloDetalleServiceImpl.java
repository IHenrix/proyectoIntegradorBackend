package pe.edu.utp.pasajeya.app.service.impl;

import pe.edu.utp.pasajeya.app.dto.PrecioPuntoDTO;
import pe.edu.utp.pasajeya.app.dto.PrediccionPuntoDTO;
import pe.edu.utp.pasajeya.app.dto.VueloDetalleDTO;
import pe.edu.utp.pasajeya.app.model.HistorialPrecio;
import pe.edu.utp.pasajeya.app.model.Tarifa;
import pe.edu.utp.pasajeya.app.model.Vuelo;
import pe.edu.utp.pasajeya.app.repository.HistorialPrecioRepository;
import pe.edu.utp.pasajeya.app.repository.TarifaRepository;
import pe.edu.utp.pasajeya.app.service.VueloDetalleService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class VueloDetalleServiceImpl implements VueloDetalleService {

    private final TarifaRepository tarifaRepo;
    private final HistorialPrecioRepository historialRepo;

    public VueloDetalleServiceImpl(TarifaRepository tarifaRepo,
                                   HistorialPrecioRepository historialRepo) {
        this.tarifaRepo = tarifaRepo;
        this.historialRepo = historialRepo;
    }

    @Override
    public VueloDetalleDTO obtenerDetalle(Long tarifaId) {
        Tarifa tarifa = tarifaRepo.findById(tarifaId.intValue())
                .orElseThrow(() -> new RuntimeException("Tarifa no encontrada"));
        Vuelo vuelo = tarifa.getVuelo();

        LocalDateTime desde = LocalDateTime.now().minusDays(30);
        List<HistorialPrecio> historial = historialRepo
                .findByVueloIdAndTipoTarifaAndFechaCapturaGreaterThanEqualOrderByFechaCapturaAsc(
                        vuelo.getId(), tarifa.getTipo(), desde);

        List<PrecioPuntoDTO> historico = historial.stream()
                .map(h -> new PrecioPuntoDTO(
                        h.getFechaCaptura().toLocalDate().toString(),
                        h.getPrecio().doubleValue()))
                .toList();

        List<PrediccionPuntoDTO> prediccion = predecir(historial, tarifa.getPrecio().doubleValue());
        String recomendacion = recomendar(historial, tarifa.getPrecio().doubleValue(), prediccion);

        LocalTime horaLlegada = vuelo.getHoraSalida().plusMinutes(vuelo.getDuracionMin());
        return new VueloDetalleDTO(
                tarifa.getId().longValue(),
                vuelo.getId(),
                nombreCorto(vuelo.getAerolinea().getNombre()),
                vuelo.getOrigen().trim(),
                vuelo.getDestino().trim(),
                vuelo.getFechaSalida().toString(),
                formatearHora(vuelo.getHoraSalida()),
                formatearHora(horaLlegada),
                formatearDuracion(vuelo.getDuracionMin()),
                tarifa.getPrecio().doubleValue(),
                tarifa.getTipo(),
                tarifa.getEquipajeBodegaKg() > 0,
                tarifa.getEquipajeBodegaKg(),
                tarifa.getEquipajeManoKg(),
                tarifa.getPermiteReembolso(),
                tarifa.getAsientoSeleccionable(),
                calcularSemaforo(historial, tarifa.getPrecio().doubleValue()),
                vuelo.getAerolinea().getUrlWeb() != null ? vuelo.getAerolinea().getUrlWeb() : "#",
                historico,
                prediccion,
                recomendacion
        );
    }

    private List<PrediccionPuntoDTO> predecir(List<HistorialPrecio> historial, double precioActual) {
        double slope = 0.0;
        if (historial.size() >= 2) {
            int n = historial.size();
            double sumX = 0.0;
            double sumY = 0.0;
            double sumXY = 0.0;
            double sumXX = 0.0;
            for (int i = 0; i < n; i++) {
                double y = historial.get(i).getPrecio().doubleValue();
                sumX += i;
                sumY += y;
                sumXY += i * y;
                sumXX += i * i;
            }
            double denominator = n * sumXX - sumX * sumX;
            if (denominator != 0) {
                slope = (n * sumXY - sumX * sumY) / denominator;
            }
        }

        List<PrediccionPuntoDTO> result = new ArrayList<>();
        LocalDate baseDate = LocalDate.now();
        for (int day = 1; day <= 7; day++) {
            double predicted = Math.max(1.0, precioActual + slope * day);
            result.add(new PrediccionPuntoDTO(baseDate.plusDays(day).toString(), redondear(predicted)));
        }
        return result;
    }

    private String recomendar(List<HistorialPrecio> historial,
                              double precioActual,
                              List<PrediccionPuntoDTO> prediccion) {
        double promedio = historial.stream()
                .mapToDouble(h -> h.getPrecio().doubleValue())
                .average()
                .orElse(precioActual);
        double precioFinalEstimado = prediccion.isEmpty()
                ? precioActual
                : prediccion.get(prediccion.size() - 1).precioEstimado();

        if (precioActual <= promedio * 0.95) {
            return "Comprar ahora: el precio esta por debajo del promedio reciente.";
        }
        if (precioFinalEstimado < precioActual * 0.97) {
            return "Esperar: la tendencia estima una posible baja en los proximos dias.";
        }
        return "Monitorear: el precio esta cerca del promedio historico.";
    }

    private String calcularSemaforo(List<HistorialPrecio> historial, double precioActual) {
        double promedio = historial.stream()
                .mapToDouble(h -> h.getPrecio().doubleValue())
                .average()
                .orElse(precioActual);
        if (precioActual < promedio * 0.95) return "verde";
        if (precioActual > promedio * 1.05) return "rojo";
        return "amarillo";
    }

    private double redondear(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String nombreCorto(String nombre) {
        String upper = nombre.toUpperCase();
        if (upper.contains("LATAM")) return "LATAM";
        if (upper.contains("SKY")) return "Sky";
        if (upper.contains("JETSMART")) return "JetSmart";
        return nombre;
    }

    private String formatearHora(LocalTime hora) {
        return String.format("%02d:%02d", hora.getHour(), hora.getMinute());
    }

    private String formatearDuracion(int minutos) {
        int horas = minutos / 60;
        int mins = minutos % 60;
        return horas > 0 ? String.format("%dh %02dm", horas, mins) : String.format("%dm", mins);
    }
}
