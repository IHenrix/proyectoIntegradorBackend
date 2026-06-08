package pe.edu.utp.pasajeya.app.repository;

import pe.edu.utp.pasajeya.app.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonaRepository extends JpaRepository<Persona, Integer> {
    boolean existsByNroDocumento(String nroDocumento);
}
