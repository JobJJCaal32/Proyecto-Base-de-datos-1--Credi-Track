package gt.umg.creditrack.cliente;

import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gt.umg.creditrack.catalogo.CatalogosFormulario;
import gt.umg.creditrack.comun.error.ErroresBaseDatos;
import gt.umg.creditrack.comun.error.ReglaNegocioException;

@Controller
@RequestMapping("/empleado/clientes/{codigo}")
public class GestionClienteController {

    private final GestionClienteService gestion;
    private final CatalogosFormulario catalogos;

    public GestionClienteController(GestionClienteService gestion, CatalogosFormulario catalogos) {
        this.gestion = gestion;
        this.catalogos = catalogos;
    }

    @GetMapping("/editar")
    @PreAuthorize("hasAuthority('ACTUALIZAR_CLIENTE')")
    public String editar(@PathVariable String codigo, Model model) {
        try {
            model.addAttribute("datos", gestion.datos(codigo));
        } catch (ReglaNegocioException e) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        }
        return mostrar(codigo, model);
    }

    @PostMapping("/editar")
    @PreAuthorize("hasAuthority('ACTUALIZAR_CLIENTE')")
    public String guardar(@PathVariable String codigo, @RequestParam(required = false) String nit,
                          @RequestParam(required = false) String telefono, @RequestParam(required = false) String correo,
                          @RequestParam(required = false) Integer idMunicipio, @RequestParam(required = false) String direccion,
                          Model model) {
        GestionClienteService.DatosContacto datos = new GestionClienteService.DatosContacto(nit, telefono, correo, idMunicipio, direccion);
        try {
            gestion.actualizar(codigo, datos);
            return "redirect:/empleado/clientes/" + codigo.toUpperCase() + "?actualizado";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        } catch (DataAccessException e) {
            model.addAttribute("error", ErroresBaseDatos.mensajeParaUsuario(e).orElse("No se pudieron guardar los cambios."));
        }
        model.addAttribute("datos", datos);
        return mostrar(codigo, model);
    }

    @PostMapping("/baja")
    @PreAuthorize("hasAuthority('DAR_BAJA_CLIENTE')")
    public String baja(@PathVariable String codigo) {
        try {
            gestion.darDeBaja(codigo);
            return "redirect:/empleado/clientes/" + codigo.toUpperCase() + "?baja";
        } catch (ReglaNegocioException e) {
            return "redirect:/empleado/clientes/" + codigo.toUpperCase() + "?baja-error="
                    + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private String mostrar(String codigo, Model model) {
        model.addAttribute("codigo", codigo.toUpperCase());
        model.addAttribute("municipios", catalogos.municipiosPorDepartamento());
        return "empleado/clientes/editar";
    }
}
