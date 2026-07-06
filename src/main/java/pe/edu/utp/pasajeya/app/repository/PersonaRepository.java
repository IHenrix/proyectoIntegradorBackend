package pe.edu.utp.pasajeya.app.repository;

import pe.edu.utp.pasajeya.app.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface PersonaRepository extends JpaRepository<Persona, Integer> {
    boolean existsByNroDocumento(String nroDocumento);
    boolean existsByNroDocumentoAndIdNot(String nroDocumento, Integer id);

    /** Cuántas personas se registraron dentro del rango, para el reporte comparativo mes actual vs anterior. */
    long countByFechaRegistroBetween(LocalDateTime desde, LocalDateTime hasta);
}
