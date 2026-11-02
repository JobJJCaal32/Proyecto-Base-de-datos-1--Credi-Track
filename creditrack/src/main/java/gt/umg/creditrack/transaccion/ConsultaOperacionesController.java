package gt.umg.creditrack.transaccion;

import java.util.List;
import java.util.Set;

import org.springframework.dao.DataAccessException;
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

import gt.umg.creditrack.cliente.ClienteRepository;
import gt.umg.creditrack.comun.error.ErroresBaseDatos;
import gt.umg.creditrack.comun.web.Meses;
import gt.umg.creditrack.cuenta.CuentaRepository;
import gt.umg.creditrack.seguridad.UsuarioActual;

@Controller
@RequestMapping("/empleado/consultas")
public class ConsultaOperacionesController {

    private static final Set<String> TIPOS = Set.of("DEPOSITO", "RETIRO", "TRANSFERENCIA");
    private static final Set<String> CANALES = Set.of("CAJA", "VIRTUAL", "APERTURA");

    private final ConsultaMovimientosService movimientos;
    private final ReporteOperacionesService reportes;
    private final ClienteRepository clientes;
    private final CuentaRepository cuentas;
    private final Meses meses;

    public ConsultaOperacionesController(ConsultaMovimientosService movimientos, ReporteOperacionesService reportes,
                                         ClienteRepository clientes, CuentaRepository cuentas, Meses meses) {
        this.movimientos = movimientos;
        this.reportes = reportes;
        this.clientes = clientes;
        this.cuentas = cuentas;
        this.meses = meses;
    }

    @GetMapping("/movimientos")
    @PreAuthorize("hasAuthority('CONSULTAR_MOVIMIENTOS')")
    public String movimientos(@AuthenticationPrincipal UsuarioActual empleado,
                              @RequestParam(required = false) String cliente,
                              @RequestParam(required = false) String cuenta,
                              @RequestParam(required = false) String mes,
                              @RequestParam(required = false) String tipo, Model model) {
        String mesElegido = mes == null ? meses.ultimos(1).get(0).valor() : mes;
        String tipoElegido = tipo != null && TIPOS.contains(tipo) ? tipo : null;
        model.addAttribute("meses", meses.ultimos(12));
        model.addAttribute("cliente", cliente);
        model.addAttribute("cuenta", cuenta);
        model.addAttribute("mes", mesElegido);
        model.addAttribute("tipo", tipoElegido);
        Integer idCliente = null;
        Integer idCuenta = null;
        if (cliente != null && !cliente.isBlank()) {
            idCliente = clientes.findByCodigoCliente(cliente.trim().toUpperCase()).map(c -> c.getIdCliente()).orElse(-1);
        }
        if (cuenta != null && !cuenta.isBlank()) {
            idCuenta = cuentas.findByNumeroCuenta(cuenta.trim()).map(c -> c.getIdCuenta()).orElse(null);
            if (idCuenta == null) {
                model.addAttribute("error", "No existe la cuenta " + cuenta.trim() + ".");
                model.addAttribute("movimientos", List.of());
                return "empleado/consultas/movimientos";
            }
        }
        Meses.Rango rango = meses.rango(mesElegido);
        try {
            model.addAttribute("movimientos", movimientos.buscar(empleado.idSesionUsuario(), idCliente, idCuenta,
                    rango.desdeUtc(), rango.hastaUtc(), tipoElegido));
        } catch (DataAccessException e) {
            model.addAttribute("movimientos", List.of());
            model.addAttribute("error", ErroresBaseDatos.mensajeParaUsuario(e).orElse("No se pudieron consultar los movimientos."));
        }
        return "empleado/consultas/movimientos";
    }

    @GetMapping("/comprobantes/{numero}")
    @PreAuthorize("hasAuthority('CONSULTAR_MOVIMIENTOS')")
    public String comprobante(@PathVariable String numero, Model model) {
        List<MovimientoFila> filas = movimientos.deTransaccion(numero);
        if (filas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("filas", filas);
        model.addAttribute("principal", filas.get(0));
        model.addAttribute("nueva", false);
        model.addAttribute("volver", "/empleado/consultas/movimientos");
        return "transaccion/comprobante";
    }

    @GetMapping("/operaciones")
    @PreAuthorize("hasAuthority('CONSULTAR_REPORTES')")
    public String operaciones(@RequestParam(required = false) String sucursal,
                              @RequestParam(required = false) String canal,
                              @RequestParam(required = false) String mes, Model model) {
        String mesElegido = mes == null ? meses.ultimos(1).get(0).valor() : mes;
        String canalElegido = canal != null && CANALES.contains(canal) ? canal : null;
        Meses.Rango rango = meses.rango(mesElegido);
        model.addAttribute("meses", meses.ultimos(12));
        model.addAttribute("sucursales", reportes.sucursales());
        model.addAttribute("sucursal", sucursal);
        model.addAttribute("canal", canalElegido);
        model.addAttribute("mes", mesElegido);
        model.addAttribute("resumen", reportes.resumen(sucursal, canalElegido, rango.desdeUtc(), rango.hastaUtc()));
        model.addAttribute("operaciones", reportes.operaciones(sucursal, canalElegido, rango.desdeUtc(), rango.hastaUtc()));
        return "empleado/consultas/operaciones";
    }
}
