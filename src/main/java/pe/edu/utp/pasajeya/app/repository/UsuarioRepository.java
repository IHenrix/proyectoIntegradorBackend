package pe.edu.utp.pasajeya.app.repository;

import pe.edu.utp.pasajeya.app.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Usuario> findByPersonaId(Integer idPersona);
    long countByActivoTrue();

    /**
     * Conteo de usuarios agrupado por nombre de rol, para el dashboard admin.
     * Cada fila del resultado es Object[]{ nombreRol (String), total (Long) }.
     */
    @Query("SELECT u.rol.nombre, COUNT(u) FROM Usuario u GROUP BY u.rol.nombre")
    List<Object[]> contarPorRol();
}
