package pe.edu.utp.pasajeya.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tipo_documento")
public class TipoDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_doc")
    private Integer id;

    private String codigo;
    private String nombre;
    private Short longitud;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Short getLongitud() { return longitud; }
    public void setLongitud(Short longitud) { this.longitud = longitud; }
}
