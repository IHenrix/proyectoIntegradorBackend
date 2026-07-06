package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.AdminHistorialPrecioDTO;
import pe.edu.utp.pasajeya.app.dto.PaginaDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminHistorialPrecioService {

    /** Usado por la exportación a Excel: sin paginar, tope fijo de filas (ver impl). */
    List<AdminHistorialPrecioDTO> buscar(
            Integer idVuelo, String origen, String destino,
            LocalDateTime desde, LocalDateTime hasta);

    /** Usado por la tabla del panel admin: paginado de verdad + búsqueda por aerolínea. */
    PaginaDTO<AdminHistorialPrecioDTO> buscarPaginado(
            Integer idVuelo, String origen, String destino,
            LocalDateTime desde, LocalDateTime hasta, String q,
            int pagina, int tamano);
}
