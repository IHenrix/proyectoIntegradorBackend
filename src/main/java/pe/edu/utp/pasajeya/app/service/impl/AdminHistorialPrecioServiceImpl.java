package pe.edu.utp.pasajeya.app.service.impl;

import org.apache.commons.lang3.StringUtils;
import pe.edu.utp.pasajeya.app.dto.AdminHistorialPrecioDTO;
import pe.edu.utp.pasajeya.app.dto.PaginaDTO;
import pe.edu.utp.pasajeya.app.model.HistorialPrecio;
import pe.edu.utp.pasajeya.app.model.Vuelo;
import pe.edu.utp.pasajeya.app.repository.HistorialPrecioRepository;
import pe.edu.utp.pasajeya.app.service.AdminHistorialPrecioService;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminHistorialPrecioServiceImpl implements AdminHistorialPrecioService {

    /**
     * La tabla tiene millones de registros (job de captura corriendo cada
     * 6h desde hace meses) — sin tope, una ruta popular devuelve 200k+ filas
     * y cuelga tanto la respuesta JSON como la exportación a Excel.
     */
    private static final int LIMITE_FILAS = 1000;

    private final HistorialPrecioRepository historialRepo;

    public AdminHistorialPrecioServiceImpl(HistorialPrecioRepository historialRepo) {
        this.historialRepo = historialRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminHistorialPrecioDTO> buscar(Integer idVuelo, String origen, String destino,
                                                 LocalDateTime desde, LocalDateTime hasta) {
        return historialRepo.buscarConFiltros(idVuelo, origen, destino, desde, hasta, Limit.of(LIMITE_FILAS)).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaDTO<AdminHistorialPrecioDTO> buscarPaginado(Integer idVuelo, String origen, String destino,
                                                              LocalDateTime desde, LocalDateTime hasta, String q,
                                                              int pagina, int tamano) {
        var pageable = PageRequest.of(pagina, tamano);
        String qNormalizado = StringUtils.trimToNull(q);
        var page = historialRepo
                .buscarConFiltrosPaginado(idVuelo, origen, destino, desde, hasta, qNormalizado, pageable)
                .map(this::toDto);
        return PaginaDTO.from(page);
    }

    private AdminHistorialPrecioDTO toDto(HistorialPrecio h) {
        Vuelo v = h.getVuelo();
        return new AdminHistorialPrecioDTO(
                v.getId(),
                v.getAerolinea().getNombre(),
                v.getOrigen(),
                v.getDestino(),
                h.getPrecio(),
                h.getTipoTarifa(),
                h.getFechaCaptura().toString()
        );
    }
}
