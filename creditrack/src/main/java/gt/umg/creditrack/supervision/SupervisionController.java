package gt.umg.creditrack.supervision;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gt.umg.creditrack.comun.bd.ReintentoTransaccional;
import gt.umg.creditrack.comun.error.ErroresBaseDatos;
import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.seguridad.UsuarioActual;

@Controller
@RequestMapping("/empleado/supervision")
public class SupervisionController {

    private static final Logger log = LoggerFactory.getLogger(SupervisionController.class);

    private final SupervisionService supervision;
    private final ReintentoTransaccional reintento;

    public SupervisionController(SupervisionService supervision, ReintentoTransaccional reintento) {
        this.supervision = supervision;
        this.reintento = reintento;
    }

    @GetMapping("/autorizaciones")
    @PreAuthorize("hasAuthority('AUTORIZAR_OPERACION')")
    public String autorizaciones(@RequestParam(required = false) String resuelta, Model model) {
        if (resuelta != null) {
            model.addAttribute("aviso", "La operación " + resuelta + " quedó resuelta.");
        }
        model.addAttribute("pendientes", supervision.pendientes());
        return "empleado/supervision/autorizaciones";
    }

    @PostMapping("/autorizaciones/{numero}")
    @PreAuthorize("hasAuthority('AUTORIZAR_OPERACION')")
    public String resolver(@AuthenticationPrincipal UsuarioActual supervisor, @PathVariable String numero,
                           @RequestParam String decision, @RequestParam(required = false) String comentario, Model model) {
        try {
            reintento.ejecutar(() -> {
                supervision.resolverPendiente(supervisor, numero, "APROBAR".equals(decision), comentario);
                return numero;
            });
            return "redirect:/empleado/supervision/autorizaciones?resuelta=" + numero;
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        } catch (DataAccessException e) {
            log.warn("No se pudo resolver la autorización", e);
            model.addAttribute("error", ErroresBaseDatos.mensajeParaUsuario(e).orElse("No se pudo resolver la operación."));
        }
        return autorizaciones(null, model);
    }

    @GetMapping("/reversos")
    @PreAuthorize("hasAuthority('REVERSAR_TRANSACCION')")
    public String reversos(@RequestParam(required = false) String numero,
                           @RequestParam(required = false) String hecho, Model model) {
        if (hecho != null) {
            model.addAttribute("aviso", "Se registró el reverso " + hecho + ".");
        }
        model.addAttribute("numero", numero);
        model.addAttribute("transferencias", supervision.reversibles(numero));
        return "empleado/supervision/reversos";
    }

    @PostMapping("/reversos/{numero}")
    @PreAuthorize("hasAuthority('REVERSAR_TRANSACCION')")
    public String reversar(@AuthenticationPrincipal UsuarioActual supervisor, @PathVariable String numero,
                           @RequestParam(required = false) String motivo, Model model) {
        try {
            String reverso = reintento.ejecutar(() -> supervision.reversar(supervisor, numero, motivo));
            return "redirect:/empleado/supervision/reversos?hecho=" + reverso;
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        } catch (DataAccessException e) {
            log.warn("No se pudo reversar", e);
            model.addAttribute("error", ErroresBaseDatos.mensajeParaUsuario(e).orElse("No se pudo registrar el reverso."));
        }
        return reversos(null, null, model);
    }

    @GetMapping("/alertas")
    @PreAuthorize("hasAuthority('GESTIONAR_ALERTAS')")
    public String alertas(@RequestParam(required = false) String estado, Model model) {
        model.addAttribute("estado", estado);
        model.addAttribute("alertas", supervision.alertas(estado));
        return "empleado/supervision/alertas";
    }

    @PostMapping("/alertas/{id}")
    @PreAuthorize("hasAuthority('GESTIONAR_ALERTAS')")
    public String gestionarAlerta(@AuthenticationPrincipal UsuarioActual supervisor, @PathVariable Long id,
                                  @RequestParam String estado, @RequestParam(required = false) String resolucion, Model model) {
        try {
            supervision.gestionarAlerta(supervisor, id, EstadoAlerta.valueOf(estado), resolucion);
            return "redirect:/empleado/supervision/alertas";
        } catch (ReglaNegocioException | IllegalArgumentException e) {
            model.addAttribute("error", e instanceof ReglaNegocioException ? e.getMessage() : "Estado no válido.");
        }
        return alertas(null, model);
    }

    @GetMapping("/arqueos")
    @PreAuthorize("hasAuthority('ARQUEAR_CAJA')")
    public String arqueos(@RequestParam(required = false) String estado, Model model) {
        model.addAttribute("estado", estado);
        model.addAttribute("cajas", supervision.cajas(estado));
        return "empleado/supervision/arqueos";
    }

    @PostMapping("/arqueos/{id}")
    @PreAuthorize("hasAuthority('ARQUEAR_CAJA')")
    public String arquear(@AuthenticationPrincipal UsuarioActual supervisor, @PathVariable Integer id,
                          @RequestParam(required = false) String observaciones, Model model) {
        try {
            supervision.arquear(supervisor, id, observaciones);
            return "redirect:/empleado/supervision/arqueos";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        } catch (DataAccessException e) {
            model.addAttribute("error", ErroresBaseDatos.mensajeParaUsuario(e).orElse("No se pudo arquear la caja."));
        }
        return arqueos(null, model);
    }
}
