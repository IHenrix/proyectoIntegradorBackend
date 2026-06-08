package pe.edu.utp.pasajeya.app.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_precio")
public class HistorialPrecio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vuelo")
    private Vuelo vuelo;

    private BigDecimal precio;

    @Column(name = "tipo_tarifa")
    private String tipoTarifa;

    @Column(name = "fecha_captura")
    private LocalDateTime fechaCaptura;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Vuelo getVuelo() { return vuelo; }
    public void setVuelo(Vuelo vuelo) { this.vuelo = vuelo; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public String getTipoTarifa() { return tipoTarifa; }
    public void setTipoTarifa(String tipoTarifa) { this.tipoTarifa = tipoTarifa; }
    public LocalDateTime getFechaCaptura() { return fechaCaptura; }
    public void setFechaCaptura(LocalDateTime fechaCaptura) { this.fechaCaptura = fechaCaptura; }
}
