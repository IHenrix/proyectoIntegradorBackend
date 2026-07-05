package pe.edu.utp.pasajeya.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Solicitud de pago simulado (sin pasarela real conectada). El backend
 * ignora cualquier monto — el precio siempre se calcula desde el Plan
 * en BD para que el cliente nunca pueda alterar cuánto paga.
 */
public record PagoRequestDTO(
        @NotBlank
        @Pattern(regexp = "mensual|anual", message = "plan debe ser 'mensual' o 'anual'")
        String plan,

        @NotBlank
        @Pattern(regexp = "tarjeta_credito|tarjeta_debito|yape|plin", message = "metodo no soportado")
        String metodo,

        // Solo para tarjeta_credito / tarjeta_debito — nunca se persiste el número completo.
        String titular,
        String numeroTarjeta,
        String expira,

        String emailRecibo
) {}
