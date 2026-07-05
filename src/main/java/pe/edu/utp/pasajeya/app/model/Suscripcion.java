package pe.edu.utp.pasajeya.app.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "suscripcion")
public class Suscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_suscripcion")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_persona")
    private Persona persona;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_plan")
    private Plan plan;

    @Column(name = "id_pago_origen")
    private Integer idPagoOrigen;

    @Column(name = "precio_pagado")
    private BigDecimal precioPagado;

    @Column(name = "max_alertas_snapshot")
    private Integer maxAlertasSnapshot;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    private String estado;

    @Column(name = "metodo_pago")
    private String metodoPago;

    @Column(name = "auto_renovar")
    private Boolean autoRenovar;
}
