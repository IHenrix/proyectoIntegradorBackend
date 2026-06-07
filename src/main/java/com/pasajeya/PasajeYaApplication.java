package com.pasajeya;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "PasajeYa API",
        version = "1.0",
        description = "Microservicio de comparación de vuelos nacionales — UTP 2026"
))
public class PasajeYaApplication {

    public static void main(String[] args) {
        SpringApplication.run(PasajeYaApplication.class, args);
    }
}
