package pe.edu.utp.pasajeya.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.pasajeya.app.model.Plan;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Integer> {

    Optional<Plan> findByNombre(String nombre);
}
