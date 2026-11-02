package gt.umg.creditrack.seguridad;

import java.security.Principal;
import java.util.Set;

import gt.umg.creditrack.comun.modelo.TipoUsuario;

public record UsuarioActual(
        Long idSesionUsuario,
        Integer idUsuario,
        String username,
        TipoUsuario tipo,
        String rol,
        Integer idCliente,
        Integer idEmpleado,
        boolean requiereCambioContrasena,
        String nombre,
        Set<String> permisos) implements Principal {

    @Override
    public String getName() {
        return username;
    }

    public boolean tienePermiso(String permiso) {
        return permisos.contains(permiso);
    }

    public boolean esCliente() {
        return tipo == TipoUsuario.CLIENTE;
    }

    public boolean esEmpleado() {
        return tipo == TipoUsuario.EMPLEADO;
    }
}
