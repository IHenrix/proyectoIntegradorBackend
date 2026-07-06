package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.AdminPagoDTO;
import pe.edu.utp.pasajeya.app.dto.AdminSuscripcionDTO;
import pe.edu.utp.pasajeya.app.dto.PaginaDTO;

import java.util.List;

public interface AdminSuscripcionService {

    /** Paginado + búsqueda opcional por email/nombre del usuario dueño. */
    PaginaDTO<AdminSuscripcionDTO> listarSuscripcionesPaginado(String q, int pagina, int tamano);

    /** Listado completo sin paginar, solo para exportación a Excel. */
    List<AdminSuscripcionDTO> listarSuscripcionesTodas();

    /** Pagos: sin paginar (no fue pedido para esta tabla). */
    List<AdminPagoDTO> listarPagos();
}
