package gt.umg.creditrack.caja;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.DynamicUpdate;

@Entity
@DynamicUpdate
@Table(name = "sesion_caja")
public class SesionCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sesion_caja")
    private Integer idSesionCaja;

    @Column(name = "id_sucursal", nullable = false)
    private Integer idSucursal;

    @Column(name = "id_empleado_cajero", nullable = false)
    private Integer idEmpleadoCajero;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "monto_apertura", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "monto_cierre_declarado", precision = 15, scale = 2)
    private BigDecimal montoCierreDeclarado;

    @Column(name = "monto_cierre_calculado", precision = 15, scale = 2)
    private BigDecimal montoCierreCalculado;

    @Column(name = "diferencia", precision = 15, scale = 2)
    private BigDecimal diferencia;

    @Column(name = "cantidad_transacciones", nullable = false)
    private Integer cantidadTransacciones = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoCaja estado = EstadoCaja.ABIERTA;

    @Column(name = "id_empleado_supervisor")
    private Integer idEmpleadoSupervisor;

    @Column(name = "fecha_arqueo")
    private LocalDateTime fechaArqueo;

    @Column(name = "observaciones", length = 300)
    private String observaciones;

    public SesionCaja() {
    }

    public Integer getIdSesionCaja() {
        return idSesionCaja;
    }

    public void setIdSesionCaja(Integer idSesionCaja) {
        this.idSesionCaja = idSesionCaja;
    }

    public Integer getIdSucursal() {
        return idSucursal;
    }

    public void setIdSucursal(Integer idSucursal) {
        this.idSucursal = idSucursal;
    }

    public Integer getIdEmpleadoCajero() {
        return idEmpleadoCajero;
    }

    public void setIdEmpleadoCajero(Integer idEmpleadoCajero) {
        this.idEmpleadoCajero = idEmpleadoCajero;
    }

    public LocalDateTime getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(LocalDateTime fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public BigDecimal getMontoApertura() {
        return montoApertura;
    }

    public void setMontoApertura(BigDecimal montoApertura) {
        this.montoApertura = montoApertura;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public BigDecimal getMontoCierreDeclarado() {
        return montoCierreDeclarado;
    }

    public void setMontoCierreDeclarado(BigDecimal montoCierreDeclarado) {
        this.montoCierreDeclarado = montoCierreDeclarado;
    }

    public BigDecimal getMontoCierreCalculado() {
        return montoCierreCalculado;
    }

    public void setMontoCierreCalculado(BigDecimal montoCierreCalculado) {
        this.montoCierreCalculado = montoCierreCalculado;
    }

    public BigDecimal getDiferencia() {
        return diferencia;
    }

    public void setDiferencia(BigDecimal diferencia) {
        this.diferencia = diferencia;
    }

    public Integer getCantidadTransacciones() {
        return cantidadTransacciones;
    }

    public void setCantidadTransacciones(Integer cantidadTransacciones) {
        this.cantidadTransacciones = cantidadTransacciones;
    }

    public EstadoCaja getEstado() {
        return estado;
    }

    public void setEstado(EstadoCaja estado) {
        this.estado = estado;
    }

    public Integer getIdEmpleadoSupervisor() {
        return idEmpleadoSupervisor;
    }

    public void setIdEmpleadoSupervisor(Integer idEmpleadoSupervisor) {
        this.idEmpleadoSupervisor = idEmpleadoSupervisor;
    }

    public LocalDateTime getFechaArqueo() {
        return fechaArqueo;
    }

    public void setFechaArqueo(LocalDateTime fechaArqueo) {
        this.fechaArqueo = fechaArqueo;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
