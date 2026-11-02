package gt.umg.creditrack.sucursal;

import java.time.LocalDate;

import jakarta.validation.Valid;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.dao.DataAccessException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gt.umg.creditrack.catalogo.CatalogosFormulario;
import gt.umg.creditrack.comun.bd.ReintentoTransaccional;
import gt.umg.creditrack.comun.error.ErroresBaseDatos;
import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.seguridad.UsuarioActual;

@Controller
@RequestMapping("/empleado/admin")
public class AdministracionController {

    private final EmpleadoService empleados;
    private final SucursalService sucursales;
    private final CatalogosFormulario catalogos;
    private final ReintentoTransaccional reintento;

    public AdministracionController(EmpleadoService empleados, SucursalService sucursales, CatalogosFormulario catalogos,
                                    ReintentoTransaccional reintento) {
        this.empleados = empleados;
        this.sucursales = sucursales;
        this.catalogos = catalogos;
        this.reintento = reintento;
    }

    @InitBinder("registro")
    void recortarTextos(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping("/empleados/nuevo")
    @PreAuthorize("hasAuthority('REGISTRAR_EMPLEADO')")
    public String nuevoEmpleado(Model model) {
        RegistroEmpleadoForm form = new RegistroEmpleadoForm();
        form.setFechaIngreso(LocalDate.now());
        model.addAttribute("registro", form);
        return formularioEmpleado(model);
    }

    @PostMapping("/empleados/nuevo")
    @PreAuthorize("hasAuthority('REGISTRAR_EMPLEADO')")
    public String registrarEmpleado(@Valid @ModelAttribute("registro") RegistroEmpleadoForm form, BindingResult errores,
                                    @AuthenticationPrincipal UsuarioActual administrador, Model model) {
        if (errores.hasErrors()) {
            model.addAttribute("error", "Revise los campos marcados.");
            return formularioEmpleado(model);
        }
        try {
            model.addAttribute("resultado", reintento.ejecutar(() -> empleados.registrar(form, administrador)));
            return "empleado/admin/empleado-registrado";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        } catch (DataAccessException e) {
            model.addAttribute("error", ErroresBaseDatos.mensajeParaUsuario(e)
                    .orElse("No se pudo registrar el empleado. No se guardó ningún dato."));
        }
        return formularioEmpleado(model);
    }

    @GetMapping("/empleados")
    @PreAuthorize("hasAuthority('GESTIONAR_EMPLEADOS')")
    public String listaEmpleados(@RequestParam(required = false) String guardado, Model model) {
        if (guardado != null) {
            model.addAttribute("aviso", "Los cambios del empleado " + guardado + " se guardaron.");
        }
        model.addAttribute("empleados", empleados.listar());
        return "empleado/admin/empleados";
    }

    @GetMapping("/empleados/{codigo}")
    @PreAuthorize("hasAuthority('GESTIONAR_EMPLEADOS')")
    public String empleado(@PathVariable String codigo, Model model) {
        try {
            model.addAttribute("empleado", empleados.buscar(codigo));
        } catch (ReglaNegocioException e) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        }
        model.addAttribute("roles", empleados.rolesDeEmpleado());
        model.addAttribute("sucursales", empleados.sucursalesActivas());
        return "empleado/admin/empleado";
    }

    @PostMapping("/empleados/{codigo}")
    @PreAuthorize("hasAuthority('GESTIONAR_EMPLEADOS')")
    public String guardarEmpleado(@PathVariable String codigo, @RequestParam(required = false) String puesto,
                                  @RequestParam(required = false) Integer idSucursal, @RequestParam(required = false) Integer idRol,
                                  @AuthenticationPrincipal UsuarioActual administrador, Model model) {
        try {
            empleados.actualizar(codigo, puesto, idSucursal, idRol, administrador);
            return "redirect:/empleado/admin/empleados?guardado=" + codigo.toUpperCase();
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        }
        return empleado(codigo, model);
    }

    @PostMapping("/empleados/{codigo}/baja")
    @PreAuthorize("hasAuthority('GESTIONAR_EMPLEADOS')")
    public String bajaEmpleado(@PathVariable String codigo, @AuthenticationPrincipal UsuarioActual administrador, Model model) {
        try {
            empleados.darDeBaja(codigo, administrador);
            return "redirect:/empleado/admin/empleados?guardado=" + codigo.toUpperCase();
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        }
        return empleado(codigo, model);
    }

    @GetMapping("/sucursales")
    @PreAuthorize("hasAuthority('GESTIONAR_SUCURSALES')")
    public String sucursales(Model model) {
        model.addAttribute("sucursales", sucursales.listar());
        model.addAttribute("municipios", catalogos.municipiosPorDepartamento());
        model.addAttribute("servicio", sucursales);
        return "empleado/admin/sucursales";
    }

    @PostMapping("/sucursales")
    @PreAuthorize("hasAuthority('GESTIONAR_SUCURSALES')")
    public String crearSucursal(@RequestParam(required = false) String codigo, @RequestParam(required = false) String nombre,
                                @RequestParam(required = false) Integer idMunicipio, @RequestParam(required = false) String direccion,
                                @RequestParam(required = false) String telefono,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaApertura,
                                Model model) {
        try {
            sucursales.crear(codigo, nombre, idMunicipio, direccion, telefono, fechaApertura);
            model.addAttribute("aviso", "La sucursal " + codigo.trim().toUpperCase() + " quedó registrada.");
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        }
        return sucursales(model);
    }

    @PostMapping("/sucursales/{id}/responsable")
    @PreAuthorize("hasAuthority('GESTIONAR_SUCURSALES')")
    public String responsable(@PathVariable Integer id, @RequestParam(required = false) Integer idEmpleado, Model model) {
        try {
            sucursales.asignarResponsable(id, idEmpleado);
            model.addAttribute("aviso", "Se asignó el responsable.");
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        }
        return sucursales(model);
    }

    @PostMapping("/sucursales/{id}/estado")
    @PreAuthorize("hasAuthority('GESTIONAR_SUCURSALES')")
    public String estadoSucursal(@PathVariable Integer id, Model model) {
        try {
            sucursales.cambiarEstado(id);
            model.addAttribute("aviso", "Se actualizó el estado de la sucursal.");
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        }
        return sucursales(model);
    }

    private String formularioEmpleado(Model model) {
        model.addAttribute("generos", catalogos.generos());
        model.addAttribute("municipios", catalogos.municipiosPorDepartamento());
        model.addAttribute("roles", empleados.rolesDeEmpleado());
        model.addAttribute("sucursales", empleados.sucursalesActivas());
        return "empleado/admin/empleado-registro";
    }
}
