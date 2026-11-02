package gt.umg.creditrack.seguridad;

import java.util.List;

import gt.umg.creditrack.comun.bd.RepositorioSoloLectura;

public interface IntentoAccesoRepository extends RepositorioSoloLectura<IntentoAcceso, Long> {

    List<IntentoAcceso> findTop10ByIdUsuarioOrderByFechaHoraDesc(Integer idUsuario);
}
