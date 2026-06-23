package pe.edu.utp.pasajeya.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PasajeYa API")
                        .version("1.0")
                        .description("Comparador de vuelos nacionales — UTP 2026\n\n" +
                                "**Cómo autenticarse:**\n" +
                                "1. Llama a `POST /api/auth/login` con tu email y password\n" +
                                "2. Copia el `token` de la respuesta\n" +
                                "3. Haz click en **Authorize** (arriba a la derecha) y pega el token"))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SCHEME_NAME, new SecurityScheme()
                                .name(SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Pega aquí el token JWT (sin el prefijo 'Bearer')")));
    }
}
