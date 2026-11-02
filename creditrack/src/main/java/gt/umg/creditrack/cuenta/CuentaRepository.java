package gt.umg.creditrack.cuenta;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentaRepository extends JpaRepository<Cuenta, Integer> {

    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    List<Cuenta> findByIdClienteOrderByFechaAperturaAscNumeroCuentaAsc(Integer idCliente);
}
