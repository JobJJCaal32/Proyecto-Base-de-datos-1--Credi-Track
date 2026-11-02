package gt.umg.creditrack.supervision;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "autorizacion")
public class Autorizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_autorizacion")
    private Integer idAutorizacion;

    @Column(name = "id_transaccion", nullable = false)
    private Long idTransaccion;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo", nullable = false)
    private MotivoAutorizacion motivo;

    @Column(name = "id_empleado_supervisor", nullable = false)
    private Integer idEmpleadoSupervisor;

    @Column(name = "fecha_resolucion", nullable = false)
    private LocalDateTime fechaResolucion;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false)
    private ResultadoAutorizacion resultado;

    @Column(name = "comentario", length = 300)
    private String comentario;

    public Autorizacion() {
    }

    public Integer getIdAutorizacion() {
        return idAutorizacion;
    }

    public void setIdAutorizacion(Integer idAutorizacion) {
        this.idAutorizacion = idAutorizacion;
    }

    public Long getIdTransaccion() {
        return idTransaccion;
    }

    public void setIdTransaccion(Long idTransaccion) {
        this.idTransaccion = idTransaccion;
    }

    public MotivoAutorizacion getMotivo() {
        return motivo;
    }

    public void setMotivo(MotivoAutorizacion motivo) {
        this.motivo = motivo;
    }

    public Integer getIdEmpleadoSupervisor() {
        return idEmpleadoSupervisor;
    }

    public void setIdEmpleadoSupervisor(Integer idEmpleadoSupervisor) {
        this.idEmpleadoSupervisor = idEmpleadoSupervisor;
    }

    public LocalDateTime getFechaResolucion() {
        return fechaResolucion;
    }

    public void setFechaResolucion(LocalDateTime fechaResolucion) {
        this.fechaResolucion = fechaResolucion;
    }

    public ResultadoAutorizacion getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoAutorizacion resultado) {
        this.resultado = resultado;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }
}
