package gt.umg.creditrack.cuenta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gt.umg.creditrack.comun.error.ErroresBaseDatos;
import gt.umg.creditrack.comun.error.ReglaNegocioException;

@Controller
@RequestMapping("/empleado/cuentas")
public class GestionCuentaController {

    private static final Logger log = LoggerFactory.getLogger(GestionCuentaController.class);

    private final GestionCuentaService gestion;

    public GestionCuentaController(GestionCuentaService gestion) {
        this.gestion = gestion;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CONSULTAR_CUENTAS')")
    public String buscarPorNumero(@RequestParam(required = false) String numero, Model model) {
        if (numero == null || numero.isBlank()) {
            return "empleado/cuentas/buscar";
        }
        return gestion.buscar(numero)
                .map(f -> "redirect:/empleado/clientes/" + f.codigoCliente())
                .orElseGet(() -> {
                    model.addAttribute("numero", numero.trim());
                    model.addAttribute("aviso", "No existe una cuenta con ese número.");
                    return "empleado/cuentas/buscar";
                });
    }

    @GetMapping("/estado")
    @PreAuthorize("hasAuthority('CAMBIAR_ESTADO_CUENTA')")
    public String estado(@RequestParam(required = false) String numero,
                         @RequestParam(required = false) String cambiado, Model model) {
        if (cambiado != null) {
            model.addAttribute("aviso", "El estado de la cuenta se actualizó.");
        }
        if (numero != null && !numero.isBlank()) {
            model.addAttribute("numero", numero.trim());
            gestion.buscar(numero).ifPresentOrElse(
                    f -> model.addAttribute("ficha", f),
                    () -> model.addAttribute("error", "No existe una cuenta con ese número."));
        }
        model.addAttribute("estados", gestion.estados());
        return "empleado/cuentas/estado";
    }

    @PostMapping("/estado")
    @PreAuthorize("hasAuthority('CAMBIAR_ESTADO_CUENTA')")
    public String cambiarEstado(@RequestParam String numero, @RequestParam(required = false) Integer estado, Model model) {
        try {
            gestion.cambiarEstado(numero, estado);
            return "redirect:/empleado/cuentas/estado?numero=" + numero + "&cambiado";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        } catch (DataAccessException e) {
            log.info("Cambio de estado rechazado por la base: {}", e.getMessage());
            model.addAttribute("error", ErroresBaseDatos.mensajeParaUsuario(e).orElse("No se pudo cambiar el estado de la cuenta."));
        }
        model.addAttribute("numero", numero);
        gestion.buscar(numero).ifPresent(f -> model.addAttribute("ficha", f));
        model.addAttribute("estados", gestion.estados());
        return "empleado/cuentas/estado";
    }
}
