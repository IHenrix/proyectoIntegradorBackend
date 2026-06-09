package pe.edu.utp.pasajeya.app.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "plan")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plan")
    private Integer id;

    private String nombre;

    @Column(name = "precio_mensual")
    private BigDecimal precioMensual;

    @Column(name = "duracion_dias")
    private Integer duracionDias;

    @Column(name = "max_alertas")
    private Integer maxAlertas;

    @Column(name = "dias_prediccion")
    private Integer diasPrediccion;

    private Boolean activo;
}
