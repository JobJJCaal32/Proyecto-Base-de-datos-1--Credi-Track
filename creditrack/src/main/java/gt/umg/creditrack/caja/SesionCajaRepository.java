package gt.umg.creditrack.caja;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SesionCajaRepository extends JpaRepository<SesionCaja, Integer> {

    Optional<SesionCaja> findByIdEmpleadoCajeroAndEstado(Integer idEmpleadoCajero, EstadoCaja estado);
}
