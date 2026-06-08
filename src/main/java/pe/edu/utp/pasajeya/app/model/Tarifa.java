package pe.edu.utp.pasajeya.app.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

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

    private String tipo;
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

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Vuelo getVuelo() { return vuelo; }
    public void setVuelo(Vuelo vuelo) { this.vuelo = vuelo; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public Integer getEquipajeBodegaKg() { return equipajeBodegaKg; }
    public void setEquipajeBodegaKg(Integer equipajeBodegaKg) { this.equipajeBodegaKg = equipajeBodegaKg; }
    public Integer getEquipajeManoKg() { return equipajeManoKg; }
    public void setEquipajeManoKg(Integer equipajeManoKg) { this.equipajeManoKg = equipajeManoKg; }
    public BigDecimal getCostoCambioFecha() { return costoCambioFecha; }
    public void setCostoCambioFecha(BigDecimal costoCambioFecha) { this.costoCambioFecha = costoCambioFecha; }
    public Boolean getPermiteReembolso() { return permiteReembolso; }
    public void setPermiteReembolso(Boolean permiteReembolso) { this.permiteReembolso = permiteReembolso; }
    public Boolean getAsientoSeleccionable() { return asientoSeleccionable; }
    public void setAsientoSeleccionable(Boolean asientoSeleccionable) { this.asientoSeleccionable = asientoSeleccionable; }
}
