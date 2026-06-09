package pe.edu.utp.pasajeya.app.repository;

import pe.edu.utp.pasajeya.app.model.HistorialPrecio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HistorialPrecioRepository extends JpaRepository<HistorialPrecio, Integer> {

    @Query("SELECT AVG(h.precio) FROM HistorialPrecio h " +
           "WHERE h.vuelo.id = :idVuelo " +
           "AND h.tipoTarifa = :tipoTarifa " +
           "AND h.fechaCaptura >= :desde")
    Optional<Double> calcularPromedio(
            @Param("idVuelo")    Integer idVuelo,
            @Param("tipoTarifa") String tipoTarifa,
            @Param("desde")      LocalDateTime desde);

    List<HistorialPrecio> findByVueloIdAndTipoTarifaAndFechaCapturaGreaterThanEqualOrderByFechaCapturaAsc(
            Integer idVuelo,
            String tipoTarifa,
            LocalDateTime desde);
}
