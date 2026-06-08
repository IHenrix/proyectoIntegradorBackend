package pe.edu.utp.pasajeya.app.repository;

import pe.edu.utp.pasajeya.app.model.Tarifa;
import pe.edu.utp.pasajeya.app.model.Vuelo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TarifaRepository extends JpaRepository<Tarifa, Integer> {

    List<Tarifa> findByVueloOrderByPrecioAsc(Vuelo vuelo);
}
