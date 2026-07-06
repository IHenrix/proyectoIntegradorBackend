package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.AdminUsuarioDetalleDTO;
import pe.edu.utp.pasajeya.app.dto.AdminUsuarioListadoDTO;
import pe.edu.utp.pasajeya.app.dto.CrearUsuarioRequestDTO;
import pe.edu.utp.pasajeya.app.dto.EditarUsuarioRequestDTO;
import pe.edu.utp.pasajeya.app.dto.PaginaDTO;

import java.util.List;

public interface AdminUsuarioService {

    /** Listado paginado (10 por página desde el controller) con búsqueda opcional por email/nombre. */
    PaginaDTO<AdminUsuarioListadoDTO> listar(int pagina, int tamano, String busqueda);

    /** Listado completo sin paginar, solo para exportación a Excel (la cantidad de usuarios es manejable). */
    List<AdminUsuarioListadoDTO> listarTodos();

    AdminUsuarioDetalleDTO obtenerDetalle(Integer idUsuario);

    /** Crea un usuario nuevo desde el panel admin. A diferencia del registro público, el admin elige el rol libremente. */
    AdminUsuarioListadoDTO crear(CrearUsuarioRequestDTO dto);

    /** Edita un usuario existente. Si dto.password() viene en blanco, no se cambia el hash actual. */
    AdminUsuarioListadoDTO editar(Integer idUsuario, EditarUsuarioRequestDTO dto);

    /**
     * Cambia el rol de un usuario. emailAdminActual es quien hace la petición
     * — se usa para impedir que un admin se auto-cambie el rol y quede
     * bloqueado del propio panel que está usando.
     */
    AdminUsuarioListadoDTO cambiarRol(Integer idUsuario, String nuevoRol, String emailAdminActual);

    /**
     * Activa/desactiva un usuario. emailAdminActual es quien hace la petición
     * — se usa para impedir que un admin se auto-desactive.
     */
    AdminUsuarioListadoDTO cambiarEstadoActivo(Integer idUsuario, boolean activo, String emailAdminActual);
}
