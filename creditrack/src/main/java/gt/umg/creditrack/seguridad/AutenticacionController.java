package gt.umg.creditrack.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gt.umg.creditrack.comun.error.ReglaNegocioException;

@Controller
public class AutenticacionController {

    private final SesionService sesiones;
    private final CambioContrasenaService cambioContrasena;
    private final CookieSesion cookie;

    public AutenticacionController(SesionService sesiones, CambioContrasenaService cambioContrasena, CookieSesion cookie) {
        this.sesiones = sesiones;
        this.cambioContrasena = cambioContrasena;
        this.cookie = cookie;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String sesion,
                        @RequestParam(required = false) String salida,
                        Authentication autenticacion, Model model) {
        if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioActual) {
            return "redirect:/";
        }
        if (salida != null) {
            model.addAttribute("aviso", "Cerró sesión correctamente.");
        } else if ("expirada".equals(sesion)) {
            model.addAttribute("aviso", "Su sesión se cerró por inactividad. Ingrese de nuevo.");
        } else if ("bloqueada".equals(sesion)) {
            model.addAttribute("error", "Su usuario fue bloqueado o desactivado. Comuníquese con el banco.");
        } else if ("cerrada".equals(sesion)) {
            model.addAttribute("aviso", "Su sesión ya no es válida. Ingrese de nuevo.");
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String ingresar(@RequestParam(defaultValue = "") String username,
                           @RequestParam(defaultValue = "") String contrasena,
                           HttpServletRequest request, HttpServletResponse response, Model model) {
        ResultadoLogin resultado = sesiones.iniciarSesion(username, contrasena,
                request.getRemoteAddr(), DescripcionEquipo.desde(request.getHeader("User-Agent")));
        if (resultado.exitoso()) {
            cookie.escribir(response, resultado.token());
            return resultado.requiereCambioContrasena() ? "redirect:" + TokenSesionFilter.RUTA_CAMBIO_CONTRASENA : "redirect:/";
        }
        model.addAttribute("error", resultado.mensaje());
        model.addAttribute("username", username.trim());
        return "auth/login";
    }

    @PostMapping("/logout")
    public String salir(HttpServletRequest request, HttpServletResponse response) {
        sesiones.cerrarSesion(cookie.leer(request), MotivoCierreSesion.LOGOUT);
        cookie.borrar(response);
        return "redirect:/login?salida";
    }

    @GetMapping(TokenSesionFilter.RUTA_CAMBIO_CONTRASENA)
    public String formularioCambio(@AuthenticationPrincipal UsuarioActual usuario, Model model) {
        model.addAttribute("obligatorio", usuario.requiereCambioContrasena());
        model.addAttribute("politica", PoliticaContrasena.DESCRIPCION);
        return "auth/cambiar-contrasena";
    }

    @PostMapping(TokenSesionFilter.RUTA_CAMBIO_CONTRASENA)
    public String cambiar(@AuthenticationPrincipal UsuarioActual usuario,
                          @RequestParam(defaultValue = "") String actual,
                          @RequestParam(defaultValue = "") String nueva,
                          @RequestParam(defaultValue = "") String confirmacion,
                          Model model) {
        try {
            cambioContrasena.cambiar(usuario.idUsuario(), actual, nueva, confirmacion);
            return "redirect:/?contrasena-actualizada";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("obligatorio", usuario.requiereCambioContrasena());
            model.addAttribute("politica", PoliticaContrasena.DESCRIPCION);
            return "auth/cambiar-contrasena";
        }
    }
}
