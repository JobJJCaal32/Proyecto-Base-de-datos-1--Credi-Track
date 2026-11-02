package gt.umg.creditrack.cliente;

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

import gt.umg.creditrack.catalogo.CatalogosFormulario;
import gt.umg.creditrack.comun.bd.ReintentoTransaccional;
import gt.umg.creditrack.comun.error.ErroresBaseDatos;
import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.seguridad.UsuarioActual;

@Controller
@RequestMapping("/empleado/clientes/nuevo")
@PreAuthorize("hasAuthority('REGISTRAR_CLIENTE')")
public class RegistroClienteController {

    private static final Logger log = LoggerFactory.getLogger(RegistroClienteController.class);
    private static final String VISTA_FORMULARIO = "empleado/clientes/registro";

    private final RegistroClienteService registro;
    private final CatalogosFormulario catalogos;
    private final ReintentoTransaccional reintento;

    public RegistroClienteController(RegistroClienteService registro, CatalogosFormulario catalogos,
                                     ReintentoTransaccional reintento) {
        this.registro = registro;
        this.catalogos = catalogos;
        this.reintento = reintento;
    }

    @InitBinder("registro")
    void recortarTextos(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String formulario(Model model) {
        model.addAttribute("registro", new RegistroClienteForm());
        return mostrarFormulario(model);
    }

    @PostMapping
    public String registrar(@Valid @ModelAttribute("registro") RegistroClienteForm form, BindingResult errores,
                            @AuthenticationPrincipal UsuarioActual oficial, Model model) {
        if (errores.hasErrors()) {
            model.addAttribute("error", "Revise los campos marcados.");
            return mostrarFormulario(model);
        }
        try {
            model.addAttribute("resultado", reintento.ejecutar(() -> registro.registrar(form, oficial)));
            return "empleado/clientes/comprobante";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        } catch (DataAccessException e) {
            log.warn("No se pudo registrar el cliente", e);
            model.addAttribute("error", ErroresBaseDatos.mensajeParaUsuario(e)
                    .orElse("No se pudo registrar el cliente. No se guardó ningún dato; intente de nuevo."));
        }
        return mostrarFormulario(model);
    }

    private String mostrarFormulario(Model model) {
        model.addAttribute("generos", catalogos.generos());
        model.addAttribute("municipios", catalogos.municipiosPorDepartamento());
        model.addAttribute("tiposCuenta", catalogos.tiposCuentaActivos());
        return VISTA_FORMULARIO;
    }
}
