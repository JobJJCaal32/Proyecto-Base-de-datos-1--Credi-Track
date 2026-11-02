package gt.umg.creditrack.cuenta;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.DynamicUpdate;

@Entity
@DynamicUpdate
@Table(name = "cuenta")
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuenta")
    private Integer idCuenta;

    @Column(name = "numero_cuenta", nullable = false, length = 20)
    private String numeroCuenta;

    @Column(name = "id_cliente", nullable = false)
    private Integer idCliente;

    @Column(name = "id_tipo_cuenta", nullable = false)
    private Integer idTipoCuenta;

    @Column(name = "id_estado_cuenta", nullable = false)
    private Integer idEstadoCuenta;

    @Column(name = "id_sucursal_apertura", nullable = false)
    private Integer idSucursalApertura;

    @Column(name = "saldo_actual", nullable = false, precision = 15, scale = 2, insertable = false, updatable = false)
    private BigDecimal saldoActual;

    @Column(name = "saldo_retenido", nullable = false, precision = 15, scale = 2, insertable = false, updatable = false)
    private BigDecimal saldoRetenido;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDate fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDate fechaCierre;

    @Column(name = "id_empleado_apertura", nullable = false)
    private Integer idEmpleadoApertura;

    @Column(name = "alias", length = 50)
    private String alias;

    public Cuenta() {
    }

    public Integer getIdCuenta() {
        return idCuenta;
    }

    public void setIdCuenta(Integer idCuenta) {
        this.idCuenta = idCuenta;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public void setNumeroCuenta(String numeroCuenta) {
        this.numeroCuenta = numeroCuenta;
    }

    public Integer getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Integer idCliente) {
        this.idCliente = idCliente;
    }

    public Integer getIdTipoCuenta() {
        return idTipoCuenta;
    }

    public void setIdTipoCuenta(Integer idTipoCuenta) {
        this.idTipoCuenta = idTipoCuenta;
    }

    public Integer getIdEstadoCuenta() {
        return idEstadoCuenta;
    }

    public void setIdEstadoCuenta(Integer idEstadoCuenta) {
        this.idEstadoCuenta = idEstadoCuenta;
    }

    public Integer getIdSucursalApertura() {
        return idSucursalApertura;
    }

    public void setIdSucursalApertura(Integer idSucursalApertura) {
        this.idSucursalApertura = idSucursalApertura;
    }

    public BigDecimal getSaldoActual() {
        return saldoActual;
    }

    public BigDecimal getSaldoRetenido() {
        return saldoRetenido;
    }

    public LocalDate getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(LocalDate fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public LocalDate getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDate fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public Integer getIdEmpleadoApertura() {
        return idEmpleadoApertura;
    }

    public void setIdEmpleadoApertura(Integer idEmpleadoApertura) {
        this.idEmpleadoApertura = idEmpleadoApertura;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }
}
