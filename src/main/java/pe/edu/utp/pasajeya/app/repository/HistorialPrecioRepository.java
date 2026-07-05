package pe.edu.utp.pasajeya.app.repository;

import pe.edu.utp.pasajeya.app.model.HistorialPrecio;
import org.springframework.data.domain.Limit;
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

    /**
     * Búsqueda flexible para el panel admin: cada filtro es opcional (NULL =
     * no se aplica esa condición), permitiendo filtrar por vuelo exacto, por
     * ruta (origen/destino) y/o por rango de fechas de captura.
     *
     * Los parámetros van con CAST explícito porque el driver JDBC de
     * PostgreSQL no puede inferir el tipo de un parámetro que llega como
     * NULL sin contexto (falla con "no se pudo determinar el tipo del
     * parámetro" en :desde/:hasta al no venir filtro de fecha) — el CAST le
     * da el tipo explícito y Postgres deja de necesitar inferirlo.
     *
     * ORDER BY DESC + Limit: la tabla tiene millones de filas (el job de
     * captura corre cada 6h desde hace meses) — sin límite, una ruta popular
     * como LIM-CUZ devuelve 200k+ filas y cuelga tanto la respuesta JSON como
     * la exportación a Excel. Se muestran las más recientes primero.
     */
    @Query("""
        SELECT h FROM HistorialPrecio h
        WHERE (CAST(:idVuelo AS java.lang.Integer) IS NULL OR h.vuelo.id = :idVuelo)
          AND (CAST(:origen AS java.lang.String) IS NULL OR h.vuelo.origen = :origen)
          AND (CAST(:destino AS java.lang.String) IS NULL OR h.vuelo.destino = :destino)
          AND (CAST(:desde AS java.time.LocalDateTime) IS NULL OR h.fechaCaptura >= :desde)
          AND (CAST(:hasta AS java.time.LocalDateTime) IS NULL OR h.fechaCaptura <= :hasta)
        ORDER BY h.fechaCaptura DESC
        """)
    List<HistorialPrecio> buscarConFiltros(
            @Param("idVuelo") Integer idVuelo,
            @Param("origen") String origen,
            @Param("destino") String destino,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Limit limit);
}
