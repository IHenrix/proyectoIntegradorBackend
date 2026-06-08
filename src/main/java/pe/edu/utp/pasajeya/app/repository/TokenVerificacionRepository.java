package pe.edu.utp.pasajeya.app.repository;

import pe.edu.utp.pasajeya.app.model.TokenVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TokenVerificacionRepository extends JpaRepository<TokenVerificacion, Integer> {
    Optional<TokenVerificacion> findByToken(String token);
}
