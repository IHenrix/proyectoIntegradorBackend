package pe.edu.utp.pasajeya.app.model;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "aerolinea")
public class Aerolinea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aerolinea")
    private Integer id;

    private String nombre;
    private String codigo;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "url_web")
    private String urlWeb;
}
