package pe.edu.utp.pasajeya.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.edu.utp.pasajeya.app.model.Pago;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Integer> {

    Optional<Pago> findById(Integer id);

    List<Pago> findAllByOrderByFechaPagoDesc();

    /** Suma de todos los pagos aprobados, para el total de ingresos del dashboard admin. */
    @Query("SELECT SUM(p.monto) FROM Pago p WHERE p.estado = 'aprobado'")
    Optional<BigDecimal> sumarIngresosTotales();
}
