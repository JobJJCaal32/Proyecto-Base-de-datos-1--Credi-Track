package gt.umg.creditrack.catalogo;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_alerta")
public class TipoAlerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_alerta")
    private Integer idTipoAlerta;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_riesgo", nullable = false)
    private NivelRiesgo nivelRiesgo;

    @Column(name = "umbral_monto", precision = 15, scale = 2)
    private BigDecimal umbralMonto;

    @Column(name = "umbral_cantidad")
    private Integer umbralCantidad;

    @Column(name = "ventana_minutos")
    private Integer ventanaMinutos;

    @Column(name = "activo", nullable = false)
    private Boolean activo = Boolean.TRUE;

    public TipoAlerta() {
    }

    public Integer getIdTipoAlerta() {
        return idTipoAlerta;
    }

    public void setIdTipoAlerta(Integer idTipoAlerta) {
        this.idTipoAlerta = idTipoAlerta;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public NivelRiesgo getNivelRiesgo() {
        return nivelRiesgo;
    }

    public void setNivelRiesgo(NivelRiesgo nivelRiesgo) {
        this.nivelRiesgo = nivelRiesgo;
    }

    public BigDecimal getUmbralMonto() {
        return umbralMonto;
    }

    public void setUmbralMonto(BigDecimal umbralMonto) {
        this.umbralMonto = umbralMonto;
    }

    public Integer getUmbralCantidad() {
        return umbralCantidad;
    }

    public void setUmbralCantidad(Integer umbralCantidad) {
        this.umbralCantidad = umbralCantidad;
    }

    public Integer getVentanaMinutos() {
        return ventanaMinutos;
    }

    public void setVentanaMinutos(Integer ventanaMinutos) {
        this.ventanaMinutos = ventanaMinutos;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
