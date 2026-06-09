package pe.edu.utp.pasajeya.app.repository;

import pe.edu.utp.pasajeya.app.model.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, Integer> {
    java.util.Optional<TipoDocumento> findByCodigo(String codigo);
}
