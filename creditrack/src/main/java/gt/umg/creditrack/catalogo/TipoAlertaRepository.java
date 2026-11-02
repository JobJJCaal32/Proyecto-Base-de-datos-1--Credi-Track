package gt.umg.creditrack.catalogo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoAlertaRepository extends JpaRepository<TipoAlerta, Integer> {

    Optional<TipoAlerta> findByNombre(String nombre);
}
