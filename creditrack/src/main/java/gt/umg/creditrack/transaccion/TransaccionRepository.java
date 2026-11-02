package gt.umg.creditrack.transaccion;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    Optional<Transaccion> findByNumeroTransaccion(String numeroTransaccion);

    Optional<Transaccion> findByReferencia(String referencia);
}
