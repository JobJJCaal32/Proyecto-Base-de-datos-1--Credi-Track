package gt.umg.creditrack.usuario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PermisoRepository extends JpaRepository<Permiso, Integer> {

    Optional<Permiso> findByNombre(String nombre);
}
