package gt.umg.creditrack.cliente;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.seguridad.UsuarioActual;

@Controller
@RequestMapping("/empleado/clientes")
@PreAuthorize("hasAuthority('BUSCAR_CLIENTE')")
public class ConsultaClienteController {

    private final ConsultaClienteService consulta;

    public ConsultaClienteController(ConsultaClienteService consulta) {
        this.consulta = consulta;
    }

    @GetMapping
    public String buscar(@RequestParam(required = false) String criterio,
                         @AuthenticationPrincipal UsuarioActual empleado, Model model) {
        if (criterio == null || criterio.isBlank()) {
            return "empleado/clientes/buscar";
        }
        model.addAttribute("criterio", criterio.trim());
        try {
            return consulta.buscar(criterio, empleado)
                    .map(c -> "redirect:/empleado/clientes/" + URLEncoder.encode(c.getCodigoCliente(), StandardCharsets.UTF_8))
                    .orElseGet(() -> {
                        model.addAttribute("aviso", "No se encontró ningún cliente con ese dato.");
                        return "empleado/clientes/buscar";
                    });
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            return "empleado/clientes/buscar";
        }
    }

    @GetMapping("/{codigo}")
    public String detalle(@PathVariable String codigo, @AuthenticationPrincipal UsuarioActual empleado,
                          @RequestParam(required = false) String actualizado, @RequestParam(required = false) String baja,
                          @RequestParam(name = "baja-error", required = false) String bajaError, Model model) {
        if (actualizado != null) {
            model.addAttribute("aviso", "Los datos del cliente se actualizaron.");
        }
        if (baja != null) {
            model.addAttribute("aviso", "El cliente quedó dado de baja y su usuario de banca virtual, inactivo.");
        }
        if (bajaError != null) {
            model.addAttribute("error", bajaError);
        }
        DetalleCliente detalle = consulta.detalle(codigo, empleado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("detalle", detalle);
        return "empleado/clientes/detalle";
    }
}
