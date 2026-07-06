package pe.edu.utp.pasajeya.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.utp.pasajeya.app.model.Pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Integer> {

    Optional<Pago> findById(Integer id);

    List<Pago> findAllByOrderByFechaPagoDesc();

    /** Suma de todos los pagos aprobados, para el total de ingresos del dashboard admin. */
    @Query("SELECT SUM(p.monto) FROM Pago p WHERE p.estado = 'aprobado'")
    Optional<BigDecimal> sumarIngresosTotales();

    /**
     * Suma de pagos aprobados agrupada por mes ("YYYY-MM"), para el gráfico de
     * ingresos mensuales del dashboard admin. Cada fila es Object[]{ mes
     * (String), total (BigDecimal) }, ordenada cronológicamente.
     */
    @Query("""
        SELECT FUNCTION('to_char', p.fechaPago, 'YYYY-MM'), SUM(p.monto)
        FROM Pago p
        WHERE p.estado = 'aprobado' AND p.fechaPago >= :desde
        GROUP BY FUNCTION('to_char', p.fechaPago, 'YYYY-MM')
        ORDER BY 1
        """)
    List<Object[]> sumarIngresosPorMes(@Param("desde") LocalDateTime desde);

    /** Suma de pagos aprobados dentro del rango, para el reporte comparativo mes actual vs anterior. */
    @Query("SELECT SUM(p.monto) FROM Pago p WHERE p.estado = 'aprobado' AND p.fechaPago BETWEEN :desde AND :hasta")
    Optional<BigDecimal> sumarIngresosEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Cantidad de pagos aprobados dentro del rango. */
    long countByEstadoAndFechaPagoBetween(String estado, LocalDateTime desde, LocalDateTime hasta);
}
