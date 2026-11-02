package gt.umg.creditrack.catalogo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoCuentaRepository extends JpaRepository<EstadoCuenta, Integer> {

    Optional<EstadoCuenta> findByNombre(String nombre);
}
