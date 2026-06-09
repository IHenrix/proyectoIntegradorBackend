package pe.edu.utp.pasajeya.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.utp.pasajeya.app.model.Suscripcion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SuscripcionRepository extends JpaRepository<Suscripcion, Integer> {

    /**
     * Suscripción vigente: estado activa Y fecha_fin >= hoy.
     * Si la fecha ya venció pero el estado no se actualizó todavía,
     * esta query no la devuelve — evita falsos positivos de premium.
     */
    @Query("""
        SELECT s FROM Suscripcion s
        WHERE s.persona.id = :idPersona
          AND s.estado = 'activa'
          AND s.fechaFin >= :hoy
        ORDER BY s.fechaFin DESC
        LIMIT 1
        """)
    Optional<Suscripcion> findVigente(
            @Param("idPersona") Integer idPersona,
            @Param("hoy") LocalDate hoy);

    /**
     * Historial completo ordenado por fecha de inicio descendente.
     * Útil para mostrar al usuario todas sus suscripciones pasadas.
     */
    @Query("""
        SELECT s FROM Suscripcion s
        WHERE s.persona.id = :idPersona
        ORDER BY s.fechaInicio DESC
        """)
    List<Suscripcion> findHistorial(@Param("idPersona") Integer idPersona);

    /**
     * Marca como 'vencida' todas las suscripciones activas cuya fecha_fin ya pasó.
     * Llamado desde el endpoint de consulta (lazy expiry) — no necesita scheduler.
     */
    @Modifying
    @Query("""
        UPDATE Suscripcion s
        SET s.estado = 'vencida'
        WHERE s.estado = 'activa'
          AND s.fechaFin < :hoy
        """)
    int expirarVencidas(@Param("hoy") LocalDate hoy);
}
