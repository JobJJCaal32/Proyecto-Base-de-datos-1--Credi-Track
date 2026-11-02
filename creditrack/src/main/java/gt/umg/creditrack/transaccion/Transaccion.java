package gt.umg.creditrack.transaccion;

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
@Table(name = "transaccion")
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transaccion")
    private Long idTransaccion;

    @Column(name = "numero_transaccion", nullable = false, length = 20)
    private String numeroTransaccion;

    @Column(name = "id_tipo_operacion", nullable = false)
    private Integer idTipoOperacion;

    @Column(name = "id_estado_transaccion", nullable = false)
    private Integer idEstadoTransaccion;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false)
    private Canal canal;

    @Column(name = "id_sesion_usuario", nullable = false)
    private Long idSesionUsuario;

    @Column(name = "id_sesion_caja")
    private Integer idSesionCaja;

    @Column(name = "concepto", length = 200)
    private String concepto;

    @Column(name = "referencia", nullable = false, length = 40)
    private String referencia;

    @Column(name = "id_transaccion_origen")
    private Long idTransaccionOrigen;

    @Column(name = "identidad_verificada", nullable = false)
    private Boolean identidadVerificada = Boolean.FALSE;

    public Transaccion() {
    }

    public Long getIdTransaccion() {
        return idTransaccion;
    }

    public void setIdTransaccion(Long idTransaccion) {
        this.idTransaccion = idTransaccion;
    }

    public String getNumeroTransaccion() {
        return numeroTransaccion;
    }

    public void setNumeroTransaccion(String numeroTransaccion) {
        this.numeroTransaccion = numeroTransaccion;
    }

    public Integer getIdTipoOperacion() {
        return idTipoOperacion;
    }

    public void setIdTipoOperacion(Integer idTipoOperacion) {
        this.idTipoOperacion = idTipoOperacion;
    }

    public Integer getIdEstadoTransaccion() {
        return idEstadoTransaccion;
    }

    public void setIdEstadoTransaccion(Integer idEstadoTransaccion) {
        this.idEstadoTransaccion = idEstadoTransaccion;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public Canal getCanal() {
        return canal;
    }

    public void setCanal(Canal canal) {
        this.canal = canal;
    }

    public Long getIdSesionUsuario() {
        return idSesionUsuario;
    }

    public void setIdSesionUsuario(Long idSesionUsuario) {
        this.idSesionUsuario = idSesionUsuario;
    }

    public Integer getIdSesionCaja() {
        return idSesionCaja;
    }

    public void setIdSesionCaja(Integer idSesionCaja) {
        this.idSesionCaja = idSesionCaja;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public Long getIdTransaccionOrigen() {
        return idTransaccionOrigen;
    }

    public void setIdTransaccionOrigen(Long idTransaccionOrigen) {
        this.idTransaccionOrigen = idTransaccionOrigen;
    }

    public Boolean getIdentidadVerificada() {
        return identidadVerificada;
    }

    public void setIdentidadVerificada(Boolean identidadVerificada) {
        this.identidadVerificada = identidadVerificada;
    }
}
