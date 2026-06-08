package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.LoginRequestDTO;
import pe.edu.utp.pasajeya.app.dto.LoginResponseDTO;
import pe.edu.utp.pasajeya.app.dto.RegistroRequestDTO;

public interface AuthService {
    void registro(RegistroRequestDTO dto);
    LoginResponseDTO login(LoginRequestDTO dto);
    void verificarEmail(String token);
}
