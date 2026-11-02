package gt.umg.creditrack.comun.web.menu;

import gt.umg.creditrack.comun.modelo.TipoUsuario;

public record OpcionMenu(
        TipoUsuario tipo,
        String etiqueta,
        String ruta,
        String permiso,
        String descripcion,
        boolean disponible) {

    public boolean esInicio() {
        return permiso == null;
    }

    public boolean protegidaPorPermiso() {
        return permiso != null && (ruta.startsWith("/banca/") || ruta.startsWith("/empleado/"));
    }
}
