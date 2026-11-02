package gt.umg.creditrack.seguridad;

import java.util.List;
import java.util.Optional;

import gt.umg.creditrack.comun.bd.RepositorioSoloLectura;

public interface SesionUsuarioRepository extends RepositorioSoloLectura<SesionUsuario, Long> {

    Optional<SesionUsuario> findByIdentificadorSesion(String identificadorSesion);

    List<SesionUsuario> findTop10ByIdUsuarioOrderByFechaInicioDesc(Integer idUsuario);
}
