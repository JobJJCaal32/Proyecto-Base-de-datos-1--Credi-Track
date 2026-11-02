package gt.umg.creditrack.usuario;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "rol_permiso")
public class RolPermiso {

    @EmbeddedId
    private RolPermisoId id;

    @Column(name = "fecha_asignacion", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fechaAsignacion;

    @Column(name = "id_usuario_asigna", nullable = false)
    private Integer idUsuarioAsigna;

    public RolPermiso() {
    }

    public RolPermiso(RolPermisoId id, Integer idUsuarioAsigna) {
        this.id = id;
        this.idUsuarioAsigna = idUsuarioAsigna;
    }

    public RolPermisoId getId() {
        return id;
    }

    public LocalDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }

    public Integer getIdUsuarioAsigna() {
        return idUsuarioAsigna;
    }
}
