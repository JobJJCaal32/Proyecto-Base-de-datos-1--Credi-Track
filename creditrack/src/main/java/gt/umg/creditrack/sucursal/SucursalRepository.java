package gt.umg.creditrack.sucursal;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SucursalRepository extends JpaRepository<Sucursal, Integer> {

    Optional<Sucursal> findByCodigoSucursal(String codigoSucursal);
}
