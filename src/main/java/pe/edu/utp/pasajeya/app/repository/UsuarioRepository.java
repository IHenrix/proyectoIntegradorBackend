package pe.edu.utp.pasajeya.app.repository;

import pe.edu.utp.pasajeya.app.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Integer id);
    Optional<Usuario> findByPersonaId(Integer idPersona);
    long countByActivoTrue();

    /**
     * Conteo de usuarios agrupado por nombre de rol, para el dashboard admin.
     * Cada fila del resultado es Object[]{ nombreRol (String), total (Long) }.
     */
    @Query("SELECT u.rol.nombre, COUNT(u) FROM Usuario u GROUP BY u.rol.nombre")
    List<Object[]> contarPorRol();

    /**
     * Listado paginado para el panel admin, con búsqueda opcional por email,
     * nombre o apellido paterno (case-insensitive, coincidencia parcial).
     * :q null u en blanco desactiva el filtro y devuelve todos.
     */
    @Query("""
        SELECT u FROM Usuario u
        WHERE (:q IS NULL OR :q = ''
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(u.persona.nombre) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(u.persona.apellidoPaterno) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<Usuario> buscar(@Param("q") String q, Pageable pageable);
}
