package gt.umg.creditrack.persona;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentidadRepository extends JpaRepository<Identidad, Integer> {

    Optional<Identidad> findByDpi(String dpi);
}
