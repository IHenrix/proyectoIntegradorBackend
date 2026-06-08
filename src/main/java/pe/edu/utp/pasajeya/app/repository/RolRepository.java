package pe.edu.utp.pasajeya.app.repository;

import pe.edu.utp.pasajeya.app.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Integer> {

    Optional<Rol> findByNombre(String nombre);
}
