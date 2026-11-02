package gt.umg.creditrack.transaccion;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoCuentaRepository extends JpaRepository<MovimientoCuenta, Long> {

    List<MovimientoCuenta> findByIdTransaccionOrderByOrden(Long idTransaccion);
}
