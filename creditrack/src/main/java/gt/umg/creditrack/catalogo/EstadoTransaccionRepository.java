package gt.umg.creditrack.catalogo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoTransaccionRepository extends JpaRepository<EstadoTransaccion, Integer> {

    Optional<EstadoTransaccion> findByNombre(String nombre);
}
