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
     * Suscripción cancelada pero aún dentro de su período pagado (fecha_fin
     * no ha pasado). Sirve para reactivar en vez de duplicar: si el usuario
     * cancela y se arrepiente antes de que venza, no debe pagar de nuevo por
     * días que ya tenía cubiertos.
     */
    @Query("""
        SELECT s FROM Suscripcion s
        WHERE s.persona.id = :idPersona
          AND s.estado = 'cancelada'
          AND s.fechaFin >= :hoy
        ORDER BY s.fechaFin DESC
        LIMIT 1
        """)
    Optional<Suscripcion> findCanceladaVigente(
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
     * Suscripciones con tarjeta que deben auto-renovarse: activas, marcadas
     * auto_renovar=true, y cuyo período pagado ya venció (o vence hoy).
     * Yape/Plin nunca tienen auto_renovar=true, así que nunca entran aquí.
     */
    @Query("""
        SELECT s FROM Suscripcion s
        WHERE s.estado = 'activa'
          AND s.autoRenovar = true
          AND s.fechaFin <= :hoy
        """)
    List<Suscripcion> findParaAutoRenovar(@Param("hoy") LocalDate hoy);

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

    /**
     * Degrada a 'usuario_free' a todo usuario premium que ya NO tenga ninguna
     * suscripción activa vigente. Se llama justo después de expirarVencidas()
     * para mantener Usuario.rol sincronizado con el estado real de sus
     * suscripciones — sin esto, el usuario seguiría con acceso premium
     * aunque su plan haya vencido o sido cancelado.
     */
    @Modifying
    @Query("""
        UPDATE Usuario u
        SET u.rol = (SELECT r FROM Rol r WHERE r.nombre = 'usuario_free')
        WHERE u.rol.nombre = 'usuario_premium'
          AND NOT EXISTS (
              SELECT 1 FROM Suscripcion s
              WHERE s.persona.id = u.persona.id
                AND s.estado = 'activa'
                AND s.fechaFin >= :hoy
          )
        """)
    int degradarSinSuscripcionVigente(@Param("hoy") LocalDate hoy);

    /** Conteo por estado ('activa'|'vencida'|'cancelada'), para el dashboard admin. */
    long countByEstado(String estado);

    /** Listado global de todas las suscripciones del sistema, para el panel admin. */
    List<Suscripcion> findAllByOrderByFechaInicioDesc();
}
