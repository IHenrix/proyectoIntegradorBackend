package pe.edu.utp.pasajeya.app.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PaginaDTO<T>(
        List<T> contenido,
        int paginaActual,
        int totalPaginas,
        long totalElementos,
        int tamanoPagina
) {
    public static <T> PaginaDTO<T> from(Page<T> page) {
        return new PaginaDTO<>(
                page.getContent(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize()
        );
    }
}
