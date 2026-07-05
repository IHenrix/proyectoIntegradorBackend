package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.PagoRequestDTO;
import pe.edu.utp.pasajeya.app.dto.SuscripcionDTO;

public interface SuscripcionService {

    /**
     * Procesa un pago (simulado, sin pasarela real conectada) y, en una sola
     * transacción: crea el registro de pago, crea la suscripción y sube el
     * rol del usuario a premium. El monto SIEMPRE se calcula desde el Plan
     * en BD — nunca se confía en un monto enviado por el cliente.
     */
    SuscripcionDTO pagar(String email, PagoRequestDTO request);

    /**
     * Cancela la suscripción activa del usuario. El usuario conserva el
     * acceso premium hasta fecha_fin (igual que Netflix/Spotify) — el rol
     * se degrada más adelante mediante expirarVencidas(), no al instante.
     */
    void cancelar(String email);
}
