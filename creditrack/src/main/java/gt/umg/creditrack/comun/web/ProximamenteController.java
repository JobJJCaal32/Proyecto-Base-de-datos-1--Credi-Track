package gt.umg.creditrack.comun.web;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

import gt.umg.creditrack.comun.web.menu.CatalogoMenu;
import gt.umg.creditrack.comun.web.menu.OpcionMenu;
import gt.umg.creditrack.seguridad.UsuarioActual;

@Controller
public class ProximamenteController {

    private final CatalogoMenu catalogoMenu;

    public ProximamenteController(CatalogoMenu catalogoMenu) {
        this.catalogoMenu = catalogoMenu;
    }

    @GetMapping({"/banca/**", "/empleado/**"})
    public String proximamente(@AuthenticationPrincipal UsuarioActual usuario, HttpServletRequest request, Model model) {
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        OpcionMenu opcion = catalogoMenu.buscar(ruta)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (opcion.tipo() != usuario.tipo() || (opcion.permiso() != null && !usuario.tienePermiso(opcion.permiso()))) {
            throw new AccessDeniedException("Sin permiso para " + ruta);
        }
        model.addAttribute("opcion", opcion);
        return "proximamente";
    }
}
