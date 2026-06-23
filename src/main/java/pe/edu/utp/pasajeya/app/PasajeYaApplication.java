package pe.edu.utp.pasajeya.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PasajeYaApplication {

    public static void main(String[] args) {
        SpringApplication.run(PasajeYaApplication.class, args);
    }
}
