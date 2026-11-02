package gt.umg.creditrack.usuario;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.seguridad.UsuarioActual;

@Controller
@RequestMapping("/empleado/admin/permisos")
@PreAuthorize("hasAuthority('ASIGNAR_PERMISOS')")
public class PermisosRolController {

    private final PermisosRolService permisos;

    public PermisosRolController(PermisosRolService permisos) {
        this.permisos = permisos;
    }

    @GetMapping
    public String permisos(@RequestParam(required = false) Integer rol, @RequestParam(required = false) Integer cambios, Model model) {
        List<Rol> roles = permisos.rolesEditables();
        Integer elegido = rol != null ? rol : roles.get(0).getIdRol();
        if (cambios != null) {
            model.addAttribute("aviso", "Se guardaron los permisos (" + cambios + " cambio(s)). Aplican desde la siguiente página que abra cada usuario.");
        }
        model.addAttribute("roles", roles);
        model.addAttribute("rol", elegido);
        model.addAttribute("permisos", permisos.permisos());
        model.addAttribute("asignados", permisos.asignados(elegido));
        return "empleado/admin/permisos";
    }

    @PostMapping("/{idRol}")
    public String guardar(@PathVariable Integer idRol, @RequestParam(name = "permiso", required = false) List<Integer> seleccion,
                          @AuthenticationPrincipal UsuarioActual administrador, Model model) {
        try {
            int cambios = permisos.guardar(idRol, seleccion, administrador);
            return "redirect:/empleado/admin/permisos?rol=" + idRol + "&cambios=" + cambios;
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            return permisos(idRol, null, model);
        }
    }
}
