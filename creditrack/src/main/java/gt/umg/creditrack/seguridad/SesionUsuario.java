package gt.umg.creditrack.seguridad;

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
@Table(name = "sesion_usuario")
public class SesionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sesion_usuario")
    private Long idSesionUsuario;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "identificador_sesion", nullable = false, length = 64)
    private String identificadorSesion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_ultimo_uso", nullable = false)
    private LocalDateTime fechaUltimoUso;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_cierre")
    private MotivoCierreSesion motivoCierre;

    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;

    @Column(name = "nombre_equipo", length = 60)
    private String nombreEquipo;

    public SesionUsuario() {
    }

    public Long getIdSesionUsuario() {
        return idSesionUsuario;
    }

    public void setIdSesionUsuario(Long idSesionUsuario) {
        this.idSesionUsuario = idSesionUsuario;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getIdentificadorSesion() {
        return identificadorSesion;
    }

    public void setIdentificadorSesion(String identificadorSesion) {
        this.identificadorSesion = identificadorSesion;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaUltimoUso() {
        return fechaUltimoUso;
    }

    public void setFechaUltimoUso(LocalDateTime fechaUltimoUso) {
        this.fechaUltimoUso = fechaUltimoUso;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public MotivoCierreSesion getMotivoCierre() {
        return motivoCierre;
    }

    public void setMotivoCierre(MotivoCierreSesion motivoCierre) {
        this.motivoCierre = motivoCierre;
    }

    public String getDireccionIp() {
        return direccionIp;
    }

    public void setDireccionIp(String direccionIp) {
        this.direccionIp = direccionIp;
    }

    public String getNombreEquipo() {
        return nombreEquipo;
    }

    public void setNombreEquipo(String nombreEquipo) {
        this.nombreEquipo = nombreEquipo;
    }
}
