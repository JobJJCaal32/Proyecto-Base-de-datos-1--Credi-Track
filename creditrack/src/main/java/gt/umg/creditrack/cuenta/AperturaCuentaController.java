package gt.umg.creditrack.cuenta;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gt.umg.creditrack.catalogo.CatalogosFormulario;
import gt.umg.creditrack.comun.bd.ReintentoTransaccional;
import gt.umg.creditrack.comun.error.ErroresBaseDatos;
import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.seguridad.UsuarioActual;

@Controller
@RequestMapping("/empleado/cuentas/nueva")
@PreAuthorize("hasAuthority('ABRIR_CUENTA')")
public class AperturaCuentaController {

    private static final Logger log = LoggerFactory.getLogger(AperturaCuentaController.class);
    private static final String VISTA_FORMULARIO = "empleado/cuentas/apertura";

    private final AperturaCuentaService apertura;
    private final CatalogosFormulario catalogos;
    private final ReintentoTransaccional reintento;

    public AperturaCuentaController(AperturaCuentaService apertura, CatalogosFormulario catalogos,
                                    ReintentoTransaccional reintento) {
        this.apertura = apertura;
        this.catalogos = catalogos;
        this.reintento = reintento;
    }

    @InitBinder("apertura")
    void recortarTextos(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String formulario(@RequestParam(required = false) String cliente, Model model) {
        AperturaCuentaForm form = new AperturaCuentaForm();
        form.setCodigoCliente(cliente);
        model.addAttribute("apertura", form);
        return mostrarFormulario(model);
    }

    @PostMapping
    public String abrir(@Valid @ModelAttribute("apertura") AperturaCuentaForm form, BindingResult errores,
                        @AuthenticationPrincipal UsuarioActual empleado, Model model) {
        if (errores.hasErrors()) {
            model.addAttribute("error", "Revise los campos marcados.");
            return mostrarFormulario(model);
        }
        try {
            model.addAttribute("resultado", reintento.ejecutar(() ->
                    apertura.abrirParaCodigo(form.getCodigoCliente(), form.getIdTipoCuenta(), form.getMontoApertura(), empleado)));
            return "empleado/cuentas/comprobante";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        } catch (DataAccessException e) {
            log.warn("No se pudo abrir la cuenta", e);
            model.addAttribute("error", ErroresBaseDatos.mensajeParaUsuario(e)
                    .orElse("No se pudo abrir la cuenta. No se guardó ningún dato; intente de nuevo."));
        }
        return mostrarFormulario(model);
    }

    private String mostrarFormulario(Model model) {
        model.addAttribute("tiposCuenta", catalogos.tiposCuentaActivos());
        return VISTA_FORMULARIO;
    }
}
