package gt.umg.creditrack.bancavirtual;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.comun.web.Meses;
import gt.umg.creditrack.cuenta.EstadoCuentaService;
import gt.umg.creditrack.seguridad.UsuarioActual;
import gt.umg.creditrack.transaccion.TransferenciaService;

@Controller
@RequestMapping("/banca/estado-cuenta")
@PreAuthorize("hasAuthority('GENERAR_ESTADO_CUENTA')")
public class EstadoCuentaController {

    private final EstadoCuentaService estados;
    private final TransferenciaService transferencias;
    private final Meses meses;

    public EstadoCuentaController(EstadoCuentaService estados, TransferenciaService transferencias, Meses meses) {
        this.estados = estados;
        this.transferencias = transferencias;
        this.meses = meses;
    }

    @GetMapping
    public String estadoCuenta(@AuthenticationPrincipal UsuarioActual cliente,
                               @RequestParam(required = false) Integer cuenta,
                               @RequestParam(required = false) String mes, Model model) {
        model.addAttribute("cuentas", transferencias.cuentasDelCliente(cliente));
        model.addAttribute("meses", meses.ultimos(12));
        model.addAttribute("cuenta", cuenta);
        model.addAttribute("mes", mes);
        if (cuenta != null && mes != null) {
            try {
                model.addAttribute("estado", estados.generar(cliente, cuenta, mes));
            } catch (ReglaNegocioException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "banca/estado-cuenta";
    }
}
