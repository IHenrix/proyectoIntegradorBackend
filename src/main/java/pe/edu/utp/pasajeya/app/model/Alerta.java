package pe.edu.utp.pasajeya.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "alerta")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vuelo")
    private Vuelo vuelo;

    @Column(name = "tipo_tarifa")
    private String tipoTarifa;

    @Column(name = "precio_objetivo")
    private BigDecimal precioObjetivo;

    private String telefono;
    private Boolean activa;

    @Column(name = "ultimo_precio_notificado")
    private BigDecimal ultimoPrecioNotificado;

    @Column(name = "fecha_ultima_notificacion")
    private LocalDateTime fechaUltimaNotificacion;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;
}
