package gt.umg.creditrack.usuario;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class RolPermisoId implements Serializable {

    @Column(name = "id_rol", nullable = false)
    private Integer idRol;

    @Column(name = "id_permiso", nullable = false)
    private Integer idPermiso;

    public RolPermisoId() {
    }

    public RolPermisoId(Integer idRol, Integer idPermiso) {
        this.idRol = idRol;
        this.idPermiso = idPermiso;
    }

    public Integer getIdRol() {
        return idRol;
    }

    public Integer getIdPermiso() {
        return idPermiso;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RolPermisoId otro)) {
            return false;
        }
        return Objects.equals(idRol, otro.idRol) && Objects.equals(idPermiso, otro.idPermiso);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idRol, idPermiso);
    }
}
