package gt.umg.creditrack.comun.web;

import java.util.Objects;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gt.umg.creditrack.comun.web.menu.CatalogoMenu;
import gt.umg.creditrack.seguridad.SesionUsuario;
import gt.umg.creditrack.seguridad.SesionUsuarioRepository;
import gt.umg.creditrack.seguridad.UsuarioActual;
import gt.umg.creditrack.sucursal.EmpleadoRepository;
import gt.umg.creditrack.sucursal.SucursalRepository;

@Controller
public class InicioController {

    private static final String PARAMETRO_CONTRASENA = "contrasena-actualizada";

    private final CatalogoMenu catalogoMenu;
    private final SesionUsuarioRepository sesiones;
    private final EmpleadoRepository empleados;
    private final SucursalRepository sucursales;

    public InicioController(CatalogoMenu catalogoMenu, SesionUsuarioRepository sesiones,
                            EmpleadoRepository empleados, SucursalRepository sucursales) {
        this.catalogoMenu = catalogoMenu;
        this.sesiones = sesiones;
        this.empleados = empleados;
        this.sucursales = sucursales;
    }

    @GetMapping("/")
    public String raiz(@AuthenticationPrincipal UsuarioActual usuario,
                       @RequestParam(name = PARAMETRO_CONTRASENA, required = false) String contrasenaActualizada) {
        String destino = CatalogoMenu.inicioDe(usuario);
        return "redirect:" + destino + (contrasenaActualizada != null ? "?" + PARAMETRO_CONTRASENA : "");
    }

    @GetMapping({CatalogoMenu.INICIO_CLIENTE, CatalogoMenu.INICIO_EMPLEADO})
    public String inicio(@AuthenticationPrincipal UsuarioActual usuario,
                         @RequestParam(name = PARAMETRO_CONTRASENA, required = false) String contrasenaActualizada,
                         Model model) {
        if (contrasenaActualizada != null) {
            model.addAttribute("aviso", "Su contraseña se actualizó correctamente.");
        }
        model.addAttribute("accesos", catalogoMenu.accesosRapidos(usuario));
        model.addAttribute("ultimoAcceso", ultimoAccesoAnterior(usuario));
        if (usuario.idEmpleado() != null) {
            empleados.findById(usuario.idEmpleado()).ifPresent(empleado -> {
                model.addAttribute("puesto", empleado.getPuesto());
                sucursales.findById(empleado.getIdSucursal())
                        .ifPresent(sucursal -> model.addAttribute("sucursal", sucursal.getNombre()));
            });
        }
        return "inicio";
    }

    private SesionUsuario ultimoAccesoAnterior(UsuarioActual usuario) {
        return sesiones.findTop10ByIdUsuarioOrderByFechaInicioDesc(usuario.idUsuario()).stream()
                .filter(s -> !Objects.equals(s.getIdSesionUsuario(), usuario.idSesionUsuario()))
                .findFirst()
                .orElse(null);
    }
}
