package gt.umg.creditrack.cliente;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    Optional<Cliente> findByCodigoCliente(String codigoCliente);

    Optional<Cliente> findByIdIdentidad(Integer idIdentidad);
}
