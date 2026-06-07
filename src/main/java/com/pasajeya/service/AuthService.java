package com.pasajeya.service;

import com.pasajeya.dto.LoginRequestDTO;
import com.pasajeya.dto.LoginResponseDTO;
import com.pasajeya.dto.RegistroRequestDTO;

public interface AuthService {
    void registro(RegistroRequestDTO dto);
    LoginResponseDTO login(LoginRequestDTO dto);
    void verificarEmail(String token);
}
