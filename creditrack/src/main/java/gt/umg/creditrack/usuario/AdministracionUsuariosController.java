package gt.umg.creditrack.usuario;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gt.umg.creditrack.comun.error.ReglaNegocioException;

@Controller
@RequestMapping("/empleado/admin/usuarios")
public class AdministracionUsuariosController {

    private final AdministracionUsuariosService administracion;

    public AdministracionUsuariosController(AdministracionUsuariosService administracion) {
        this.administracion = administracion;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('DESBLOQUEAR_USUARIO', 'RESETEAR_CONTRASENA')")
    public String lista(@RequestParam(required = false) String texto,
                        @RequestParam(required = false) String desbloqueado, Model model) {
        if (desbloqueado != null) {
            model.addAttribute("aviso", "El usuario " + desbloqueado + " quedó desbloqueado.");
        }
        return mostrar(texto, model);
    }

    @PostMapping("/{username}/desbloquear")
    @PreAuthorize("hasAuthority('DESBLOQUEAR_USUARIO')")
    public String desbloquear(@PathVariable String username, Model model) {
        try {
            administracion.desbloquear(username);
            return "redirect:/empleado/admin/usuarios?desbloqueado=" + username;
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            return mostrar(null, model);
        }
    }

    @PostMapping("/{username}/resetear")
    @PreAuthorize("hasAuthority('RESETEAR_CONTRASENA')")
    public String resetear(@PathVariable String username, Model model) {
        try {
            model.addAttribute("username", username);
            model.addAttribute("temporal", administracion.resetearContrasena(username));
            return "empleado/admin/contrasena-reseteada";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            return mostrar(null, model);
        }
    }

    private String mostrar(String texto, Model model) {
        model.addAttribute("texto", texto);
        model.addAttribute("usuarios", administracion.buscar(texto));
        return "empleado/admin/usuarios";
    }
}
