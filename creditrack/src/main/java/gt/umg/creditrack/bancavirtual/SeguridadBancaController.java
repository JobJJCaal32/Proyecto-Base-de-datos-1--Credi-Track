package gt.umg.creditrack.bancavirtual;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.seguridad.ClaveOperacionService;
import gt.umg.creditrack.seguridad.IntentoAccesoRepository;
import gt.umg.creditrack.seguridad.SesionUsuarioRepository;
import gt.umg.creditrack.seguridad.UsuarioActual;

@Controller
@RequestMapping("/banca")
public class SeguridadBancaController {

    private final ClaveOperacionService claveOperacion;
    private final SesionUsuarioRepository sesiones;
    private final IntentoAccesoRepository intentos;

    public SeguridadBancaController(ClaveOperacionService claveOperacion, SesionUsuarioRepository sesiones,
                                    IntentoAccesoRepository intentos) {
        this.claveOperacion = claveOperacion;
        this.sesiones = sesiones;
        this.intentos = intentos;
    }

    @GetMapping("/clave-operacion")
    @PreAuthorize("hasAuthority('CONFIGURAR_CLAVE_OPERACION')")
    public String formulario(@AuthenticationPrincipal UsuarioActual cliente,
                             @RequestParam(required = false) String guardada, Model model) {
        if (guardada != null) {
            model.addAttribute("aviso", "Su clave de operación quedó guardada. Se le pedirá en cada transferencia.");
        }
        model.addAttribute("configurada", claveOperacion.configurada(cliente));
        return "banca/clave-operacion";
    }

    @PostMapping("/clave-operacion")
    @PreAuthorize("hasAuthority('CONFIGURAR_CLAVE_OPERACION')")
    public String guardar(@AuthenticationPrincipal UsuarioActual cliente,
                          @RequestParam(defaultValue = "") String contrasena,
                          @RequestParam(defaultValue = "") String clave,
                          @RequestParam(defaultValue = "") String confirmacion, Model model) {
        try {
            claveOperacion.configurar(cliente, contrasena, clave, confirmacion);
            return "redirect:/banca/clave-operacion?guardada";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("configurada", claveOperacion.configurada(cliente));
            return "banca/clave-operacion";
        }
    }

    @GetMapping("/accesos")
    @PreAuthorize("hasAuthority('VER_ACCESOS')")
    public String accesos(@AuthenticationPrincipal UsuarioActual cliente, Model model) {
        model.addAttribute("sesiones", sesiones.findTop10ByIdUsuarioOrderByFechaInicioDesc(cliente.idUsuario()));
        model.addAttribute("intentos", intentos.findTop10ByIdUsuarioOrderByFechaHoraDesc(cliente.idUsuario()));
        return "banca/accesos";
    }
}
