package pe.edu.utp.pasajeya.app.repository;

import pe.edu.utp.pasajeya.app.model.Alerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AlertaRepository extends JpaRepository<Alerta, Integer> {

    List<Alerta> findByUsuarioEmailOrderByFechaCreacionDesc(String email);

    Optional<Alerta> findByIdAndUsuarioEmail(Integer id, String email);

    List<Alerta> findByActivaTrue();

    long countByActivaTrue();

    long countByUsuarioEmail(String email);

    boolean existsByUsuarioEmailAndVueloIdAndTipoTarifa(String email, Integer vueloId, String tipoTarifa);

    /**
     * Conteo de alertas activas agrupado por nombre de aerolínea del vuelo
     * asociado, para el gráfico del dashboard admin. Cada fila es
     * Object[]{ nombreAerolinea (String), total (Long) }.
     */
    @Query("""
        SELECT a.vuelo.aerolinea.nombre, COUNT(a)
        FROM Alerta a
        WHERE a.activa = true
        GROUP BY a.vuelo.aerolinea.nombre
        """)
    List<Object[]> contarActivasPorAerolinea();
}
