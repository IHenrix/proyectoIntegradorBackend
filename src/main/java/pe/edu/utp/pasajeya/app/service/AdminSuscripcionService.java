package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.AdminPagoDTO;
import pe.edu.utp.pasajeya.app.dto.AdminSuscripcionDTO;

import java.util.List;

public interface AdminSuscripcionService {

    List<AdminSuscripcionDTO> listarSuscripciones();

    List<AdminPagoDTO> listarPagos();
}
