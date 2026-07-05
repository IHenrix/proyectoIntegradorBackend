package pe.edu.utp.pasajeya.app.repository;

import pe.edu.utp.pasajeya.app.model.Alerta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertaRepository extends JpaRepository<Alerta, Integer> {

    List<Alerta> findByUsuarioEmailOrderByFechaCreacionDesc(String email);

    Optional<Alerta> findByIdAndUsuarioEmail(Integer id, String email);

    List<Alerta> findByActivaTrue();

    long countByActivaTrue();

    long countByUsuarioEmail(String email);

    boolean existsByUsuarioEmailAndVueloIdAndTipoTarifa(String email, Integer vueloId, String tipoTarifa);
}
