package pe.edu.utp.pasajeya.app.model;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "aeropuerto")
public class Aeropuerto {

    @Id
    @Column(name = "codigo", length = 3)
    private String codigo;

    private String nombre;
    private String ciudad;
    private String pais;
}
