package pe.edu.utp.pasajeya.app.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "tarifa")
public class Tarifa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tarifa")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vuelo")
    private Vuelo vuelo;

    private String     tipo;
    private BigDecimal precio;

    @Column(name = "equipaje_bodega_kg")
    private Integer equipajeBodegaKg;

    @Column(name = "equipaje_mano_kg")
    private Integer equipajeManoKg;

    @Column(name = "costo_cambio_fecha")
    private BigDecimal costoCambioFecha;

    @Column(name = "permite_reembolso")
    private Boolean permiteReembolso;

    @Column(name = "asiento_seleccionable")
    private Boolean asientoSeleccionable;
}
