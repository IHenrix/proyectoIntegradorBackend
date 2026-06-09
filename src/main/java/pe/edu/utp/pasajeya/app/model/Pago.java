package pe.edu.utp.pasajeya.app.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_persona")
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plan")
    private Plan plan;

    private BigDecimal monto;
    private String     moneda;
    private String     metodo;
    private String     estado;
    private String     pasarela;

    @Column(name = "token_pasarela")
    private String tokenPasarela;

    @Column(name = "ultimos_cuatro")
    private String ultimosCuatro;

    @Column(name = "marca_tarjeta")
    private String marcaTarjeta;

    @Column(name = "titular_tarjeta")
    private String titularTarjeta;

    @Column(name = "email_recibo")
    private String emailRecibo;

    @Column(name = "ref_interna")
    private String refInterna;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;
}
