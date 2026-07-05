package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.AdminUsuarioDetalleDTO;
import pe.edu.utp.pasajeya.app.dto.AdminUsuarioListadoDTO;

import java.util.List;

public interface AdminUsuarioService {

    List<AdminUsuarioListadoDTO> listar();

    AdminUsuarioDetalleDTO obtenerDetalle(Integer idUsuario);

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
