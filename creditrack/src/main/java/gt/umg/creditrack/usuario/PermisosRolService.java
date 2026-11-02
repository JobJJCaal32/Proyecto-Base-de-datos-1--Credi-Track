package gt.umg.creditrack.usuario;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.comun.modelo.TipoUsuario;
import gt.umg.creditrack.seguridad.UsuarioActual;

@Service
public class PermisosRolService {

    private static final String PERMISO_CLAVE = "ASIGNAR_PERMISOS";
    private static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";

    private final RolRepository roles;
    private final PermisoRepository permisos;
    private final RolPermisoRepository rolPermisos;
    private final JdbcTemplate jdbc;

    public PermisosRolService(RolRepository roles, PermisoRepository permisos, RolPermisoRepository rolPermisos, JdbcTemplate jdbc) {
        this.roles = roles;
        this.permisos = permisos;
        this.rolPermisos = rolPermisos;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<Rol> rolesEditables() {
        return roles.findAll().stream().filter(r -> r.getTipoUsuario() != TipoUsuario.SISTEMA)
                .sorted(java.util.Comparator.comparing(Rol::getNombre)).toList();
    }

    @Transactional(readOnly = true)
    public List<Permiso> permisos() {
        return permisos.findAll().stream().sorted(java.util.Comparator.comparing(Permiso::getNombre)).toList();
    }

    @Transactional(readOnly = true)
    public Set<Integer> asignados(Integer idRol) {
        Set<Integer> ids = new HashSet<>();
        rolPermisos.findByIdIdRol(idRol).forEach(rp -> ids.add(rp.getId().getIdPermiso()));
        return ids;
    }

    @Transactional
    public int guardar(Integer idRol, List<Integer> seleccion, UsuarioActual administrador) {
        Rol rol = roles.findById(idRol == null ? -1 : idRol)
                .filter(r -> r.getTipoUsuario() != TipoUsuario.SISTEMA)
                .orElseThrow(() -> new ReglaNegocioException("Seleccione un rol válido."));
        Set<Integer> nuevos = new HashSet<>(seleccion == null ? List.of() : seleccion);
        Set<Integer> validos = new HashSet<>();
        permisos.findAll().forEach(p -> validos.add(p.getIdPermiso()));
        nuevos.retainAll(validos);
        if (ROL_ADMINISTRADOR.equals(rol.getNombre())) {
            Integer clave = permisos.findByNombre(PERMISO_CLAVE).orElseThrow().getIdPermiso();
            if (!nuevos.contains(clave)) {
                throw new ReglaNegocioException("El rol ADMINISTRADOR debe conservar el permiso " + PERMISO_CLAVE + ".");
            }
        }
        Set<Integer> actuales = asignados(rol.getIdRol());
        int cambios = 0;
        for (Integer id : actuales) {
            if (!nuevos.contains(id)) {
                cambios += jdbc.update("DELETE FROM rol_permiso WHERE id_rol = ? AND id_permiso = ?", rol.getIdRol(), id);
            }
        }
        for (Integer id : nuevos) {
            if (!actuales.contains(id)) {
                cambios += jdbc.update("INSERT INTO rol_permiso (id_rol, id_permiso, id_usuario_asigna) VALUES (?, ?, ?)",
                        rol.getIdRol(), id, administrador.idUsuario());
            }
        }
        return cambios;
    }
}
