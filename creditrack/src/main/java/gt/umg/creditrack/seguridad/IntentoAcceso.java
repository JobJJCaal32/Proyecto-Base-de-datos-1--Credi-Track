package gt.umg.creditrack.seguridad;

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
@Table(name = "intento_acceso")
public class IntentoAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_intento")
    private Long idIntento;

    @Column(name = "username_intentado", nullable = false, length = 30)
    private String usernameIntentado;

    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "exitoso", nullable = false)
    private Boolean exitoso;

    @Column(name = "motivo_fallo", length = 40)
    private String motivoFallo;

    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;

    public IntentoAcceso() {
    }

    public Long getIdIntento() {
        return idIntento;
    }

    public void setIdIntento(Long idIntento) {
        this.idIntento = idIntento;
    }

    public String getUsernameIntentado() {
        return usernameIntentado;
    }

    public void setUsernameIntentado(String usernameIntentado) {
        this.usernameIntentado = usernameIntentado;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public Boolean getExitoso() {
        return exitoso;
    }

    public void setExitoso(Boolean exitoso) {
        this.exitoso = exitoso;
    }

    public String getMotivoFallo() {
        return motivoFallo;
    }

    public void setMotivoFallo(String motivoFallo) {
        this.motivoFallo = motivoFallo;
    }

    public String getDireccionIp() {
        return direccionIp;
    }

    public void setDireccionIp(String direccionIp) {
        this.direccionIp = direccionIp;
    }
}
