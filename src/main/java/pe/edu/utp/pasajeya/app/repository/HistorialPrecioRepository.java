package pe.edu.utp.pasajeya.app.repository;

import pe.edu.utp.pasajeya.app.model.HistorialPrecio;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Misma búsqueda que buscarConFiltros, pero paginada de verdad (offset +
     * tamaño) en vez de un tope fijo, más un filtro de texto libre por nombre
     * de aerolínea. Usada por la tabla paginada del panel admin; el método
     * viejo con Limit se mantiene intacto porque lo sigue usando la
     * exportación a Excel (que exporta "hasta 1000 filas", no "una página").
     */
    @Query("""
        SELECT h FROM HistorialPrecio h
        WHERE (CAST(:idVuelo AS java.lang.Integer) IS NULL OR h.vuelo.id = :idVuelo)
          AND (CAST(:origen AS java.lang.String) IS NULL OR h.vuelo.origen = :origen)
          AND (CAST(:destino AS java.lang.String) IS NULL OR h.vuelo.destino = :destino)
          AND (CAST(:desde AS java.time.LocalDateTime) IS NULL OR h.fechaCaptura >= :desde)
          AND (CAST(:hasta AS java.time.LocalDateTime) IS NULL OR h.fechaCaptura <= :hasta)
          AND (:q IS NULL OR :q = '' OR LOWER(h.vuelo.aerolinea.nombre) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY h.fechaCaptura DESC
        """)
    Page<HistorialPrecio> buscarConFiltrosPaginado(
            @Param("idVuelo") Integer idVuelo,
            @Param("origen") String origen,
            @Param("destino") String destino,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("q") String q,
            Pageable pageable);

    /**
     * Precio promedio agrupado por ruta (origen-destino) y semana ISO, para
     * el gráfico de evolución de precios del dashboard admin. Cada fila es
     * Object[]{ origen (String), destino (String), semana "IYYY-IW" (String),
     * promedio (Double) }, ordenada cronológicamente.
     */
    @Query("""
        SELECT h.vuelo.origen, h.vuelo.destino,
               FUNCTION('to_char', h.fechaCaptura, 'IYYY-IW'), AVG(h.precio)
        FROM HistorialPrecio h
        WHERE h.fechaCaptura >= :desde
        GROUP BY h.vuelo.origen, h.vuelo.destino, FUNCTION('to_char', h.fechaCaptura, 'IYYY-IW')
        ORDER BY 3
        """)
    List<Object[]> promedioSemanalPorRuta(@Param("desde") LocalDateTime desde);

    /** Cantidad de registros de historial agrupados por ruta, para elegir las rutas con más volumen. */
    @Query("""
        SELECT h.vuelo.origen, h.vuelo.destino, COUNT(h)
        FROM HistorialPrecio h
        GROUP BY h.vuelo.origen, h.vuelo.destino
        ORDER BY 3 DESC
        """)
    List<Object[]> contarPorRuta(Pageable pageable);
}
