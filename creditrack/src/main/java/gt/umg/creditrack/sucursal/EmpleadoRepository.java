package gt.umg.creditrack.sucursal;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpleadoRepository extends JpaRepository<Empleado, Integer> {

    Optional<Empleado> findByCodigoEmpleado(String codigoEmpleado);

    Optional<Empleado> findByIdIdentidad(Integer idIdentidad);
}
