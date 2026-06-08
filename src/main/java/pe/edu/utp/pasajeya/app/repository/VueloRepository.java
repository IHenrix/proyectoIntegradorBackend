package pe.edu.utp.pasajeya.app.repository;

import pe.edu.utp.pasajeya.app.model.Vuelo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface VueloRepository extends JpaRepository<Vuelo, Integer> {

    List<Vuelo> findByOrigenAndDestinoAndFechaSalida(String origen, String destino, LocalDate fechaSalida);
}
