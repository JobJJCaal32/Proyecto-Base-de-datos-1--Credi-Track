package gt.umg.creditrack.catalogo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MunicipioRepository extends JpaRepository<Municipio, Integer> {

    List<Municipio> findByIdDepartamentoOrderByNombre(Integer idDepartamento);
}
