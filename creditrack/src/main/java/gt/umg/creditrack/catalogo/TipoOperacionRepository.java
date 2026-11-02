package gt.umg.creditrack.catalogo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoOperacionRepository extends JpaRepository<TipoOperacion, Integer> {

    Optional<TipoOperacion> findByNombre(String nombre);
}
