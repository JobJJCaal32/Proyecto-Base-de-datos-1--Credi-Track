package gt.umg.creditrack.auditoria;

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
@Table(name = "bitacora_consulta")
public class BitacoraConsulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bitacora_consulta")
    private Long idBitacoraConsulta;

    @Column(name = "id_sesion_usuario", nullable = false)
    private Long idSesionUsuario;

    @Column(name = "username", nullable = false, length = 30)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_consulta", nullable = false)
    private TipoConsulta tipoConsulta;

    @Column(name = "id_cuenta")
    private Integer idCuenta;

    @Column(name = "id_cliente")
    private Integer idCliente;

    @Column(name = "criterio", length = 200)
    private String criterio;

    @Column(name = "cantidad_registros", nullable = false)
    private Integer cantidadRegistros = 0;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    public BitacoraConsulta() {
    }

    public Long getIdBitacoraConsulta() {
        return idBitacoraConsulta;
    }

    public void setIdBitacoraConsulta(Long idBitacoraConsulta) {
        this.idBitacoraConsulta = idBitacoraConsulta;
    }

    public Long getIdSesionUsuario() {
        return idSesionUsuario;
    }

    public void setIdSesionUsuario(Long idSesionUsuario) {
        this.idSesionUsuario = idSesionUsuario;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public TipoConsulta getTipoConsulta() {
        return tipoConsulta;
    }

    public void setTipoConsulta(TipoConsulta tipoConsulta) {
        this.tipoConsulta = tipoConsulta;
    }

    public Integer getIdCuenta() {
        return idCuenta;
    }

    public void setIdCuenta(Integer idCuenta) {
        this.idCuenta = idCuenta;
    }

    public Integer getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Integer idCliente) {
        this.idCliente = idCliente;
    }

    public String getCriterio() {
        return criterio;
    }

    public void setCriterio(String criterio) {
        this.criterio = criterio;
    }

    public Integer getCantidadRegistros() {
        return cantidadRegistros;
    }

    public void setCantidadRegistros(Integer cantidadRegistros) {
        this.cantidadRegistros = cantidadRegistros;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }
}
