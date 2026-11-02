package gt.umg.creditrack.catalogo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneroRepository extends JpaRepository<Genero, Integer> {

    Optional<Genero> findByNombre(String nombre);
}
