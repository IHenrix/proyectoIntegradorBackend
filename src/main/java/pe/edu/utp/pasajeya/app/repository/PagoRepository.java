package pe.edu.utp.pasajeya.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.pasajeya.app.model.Pago;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Integer> {

    Optional<Pago> findById(Integer id);
}
