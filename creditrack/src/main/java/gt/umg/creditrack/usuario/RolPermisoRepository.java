package gt.umg.creditrack.usuario;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolPermisoRepository extends JpaRepository<RolPermiso, RolPermisoId> {

    List<RolPermiso> findByIdIdRol(Integer idRol);

    @Query("select p.nombre from RolPermiso rp join Permiso p on p.idPermiso = rp.id.idPermiso where rp.id.idRol = :idRol order by p.nombre")
    List<String> nombresDePermisos(@Param("idRol") Integer idRol);
}
