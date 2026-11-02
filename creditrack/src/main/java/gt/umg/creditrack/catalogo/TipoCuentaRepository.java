package gt.umg.creditrack.catalogo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoCuentaRepository extends JpaRepository<TipoCuenta, Integer> {

    Optional<TipoCuenta> findByNombre(String nombre);

    List<TipoCuenta> findByActivoTrueOrderByNombre();
}
