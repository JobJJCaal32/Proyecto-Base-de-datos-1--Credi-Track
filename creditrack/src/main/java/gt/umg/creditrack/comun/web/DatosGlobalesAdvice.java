package gt.umg.creditrack.comun.web;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import gt.umg.creditrack.comun.web.menu.CatalogoMenu;
import gt.umg.creditrack.comun.web.menu.SeccionMenu;
import gt.umg.creditrack.seguridad.UsuarioActual;

@ControllerAdvice
public class DatosGlobalesAdvice {

    private final CatalogoMenu catalogoMenu;

    public DatosGlobalesAdvice(CatalogoMenu catalogoMenu) {
        this.catalogoMenu = catalogoMenu;
    }

    @ModelAttribute
    public void datosGlobales(Authentication autenticacion, HttpServletRequest request, Model model) {
        UsuarioActual usuario = null;
        if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioActual actual) {
            usuario = actual;
        }
        boolean mostrarMenu = usuario != null && !usuario.requiereCambioContrasena();
        List<SeccionMenu> menu = mostrarMenu ? catalogoMenu.para(usuario) : List.of();
        model.addAttribute("usuarioActual", usuario);
        model.addAttribute("mostrarMenu", mostrarMenu);
        model.addAttribute("menu", menu);
        model.addAttribute("rutaActual", request.getRequestURI().substring(request.getContextPath().length()));
        model.addAttribute("inicio", usuario == null ? "/login" : CatalogoMenu.inicioDe(usuario));
    }
}
