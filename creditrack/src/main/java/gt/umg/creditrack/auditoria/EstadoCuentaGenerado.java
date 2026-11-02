package gt.umg.creditrack.auditoria;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "estado_cuenta_generado")
public class EstadoCuentaGenerado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_cuenta_gen")
    private Integer idEstadoCuentaGen;

    @Column(name = "id_cuenta", nullable = false)
    private Integer idCuenta;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "saldo_inicial", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoInicial;

    @Column(name = "total_cargos", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalCargos;

    @Column(name = "total_abonos", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAbonos;

    @Column(name = "saldo_final", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoFinal;

    @Column(name = "cantidad_movimientos", nullable = false)
    private Integer cantidadMovimientos;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "id_usuario_genera", nullable = false)
    private Integer idUsuarioGenera;

    public EstadoCuentaGenerado() {
    }

    public Integer getIdEstadoCuentaGen() {
        return idEstadoCuentaGen;
    }

    public void setIdEstadoCuentaGen(Integer idEstadoCuentaGen) {
        this.idEstadoCuentaGen = idEstadoCuentaGen;
    }

    public Integer getIdCuenta() {
        return idCuenta;
    }

    public void setIdCuenta(Integer idCuenta) {
        this.idCuenta = idCuenta;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(BigDecimal saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public BigDecimal getTotalCargos() {
        return totalCargos;
    }

    public void setTotalCargos(BigDecimal totalCargos) {
        this.totalCargos = totalCargos;
    }

    public BigDecimal getTotalAbonos() {
        return totalAbonos;
    }

    public void setTotalAbonos(BigDecimal totalAbonos) {
        this.totalAbonos = totalAbonos;
    }

    public BigDecimal getSaldoFinal() {
        return saldoFinal;
    }

    public void setSaldoFinal(BigDecimal saldoFinal) {
        this.saldoFinal = saldoFinal;
    }

    public Integer getCantidadMovimientos() {
        return cantidadMovimientos;
    }

    public void setCantidadMovimientos(Integer cantidadMovimientos) {
        this.cantidadMovimientos = cantidadMovimientos;
    }

    public LocalDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(LocalDateTime fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }

    public Integer getIdUsuarioGenera() {
        return idUsuarioGenera;
    }

    public void setIdUsuarioGenera(Integer idUsuarioGenera) {
        this.idUsuarioGenera = idUsuarioGenera;
    }
}
