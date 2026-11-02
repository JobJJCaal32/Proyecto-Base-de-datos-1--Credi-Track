package gt.umg.creditrack.caja;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import gt.umg.creditrack.comun.bd.ReintentoTransaccional;
import gt.umg.creditrack.comun.error.ErroresBaseDatos;
import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.seguridad.UsuarioActual;
import gt.umg.creditrack.transaccion.ConsultaMovimientosService;
import gt.umg.creditrack.transaccion.MovimientoFila;
import gt.umg.creditrack.transaccion.RegistroTransacciones;
import gt.umg.creditrack.transaccion.Transaccion;

@Controller
@RequestMapping("/empleado/caja")
public class CajaController {

    private static final Logger log = LoggerFactory.getLogger(CajaController.class);

    private final CajaService caja;
    private final ReintentoTransaccional reintento;
    private final ConsultaMovimientosService movimientos;
    private final RegistroTransacciones registro;

    public CajaController(CajaService caja, ReintentoTransaccional reintento, ConsultaMovimientosService movimientos,
                          RegistroTransacciones registro) {
        this.caja = caja;
        this.reintento = reintento;
        this.movimientos = movimientos;
        this.registro = registro;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ABRIR_CAJA')")
    public String miCaja(@AuthenticationPrincipal UsuarioActual cajero,
                         @RequestParam(required = false) String abierta, Model model) {
        if (abierta != null) {
            model.addAttribute("aviso", "Su caja quedó abierta.");
        }
        caja.resumenAbierta(cajero).ifPresent(r -> model.addAttribute("resumen", r));
        model.addAttribute("limite", caja.limiteRetiro());
        return "empleado/caja/inicio";
    }

    @PostMapping("/abrir")
    @PreAuthorize("hasAuthority('ABRIR_CAJA')")
    public String abrir(@AuthenticationPrincipal UsuarioActual cajero,
                        @RequestParam(required = false) BigDecimal montoApertura, Model model) {
        try {
            caja.abrir(cajero, montoApertura);
            return "redirect:/empleado/caja?abierta";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        } catch (DataAccessException e) {
            model.addAttribute("error", ErroresBaseDatos.mensajeParaUsuario(e).orElse("No se pudo abrir la caja."));
        }
        return miCaja(cajero, null, model);
    }

    @PostMapping("/cerrar")
    @PreAuthorize("hasAuthority('CERRAR_CAJA')")
    public String cerrar(@AuthenticationPrincipal UsuarioActual cajero,
                         @RequestParam(required = false) BigDecimal montoDeclarado,
                         @RequestParam(required = false) String observaciones, Model model) {
        try {
            model.addAttribute("cierre", caja.cerrar(cajero, montoDeclarado, observaciones));
            return "empleado/caja/cierre";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        }
        return miCaja(cajero, null, model);
    }

    @GetMapping({"/deposito", "/retiro", "/transferencia"})
    public String formulario(@AuthenticationPrincipal UsuarioActual cajero,
                             jakarta.servlet.http.HttpServletRequest request, Model model) {
        String tipo = request.getRequestURI().substring(request.getRequestURI().lastIndexOf('/') + 1);
        return mostrarFormulario(cajero, tipo, model);
    }

    @PostMapping("/deposito")
    @PreAuthorize("hasAuthority('REALIZAR_DEPOSITO')")
    public String depositar(@AuthenticationPrincipal UsuarioActual cajero,
                            @RequestParam(defaultValue = "") String cuenta,
                            @RequestParam(required = false) BigDecimal monto,
                            @RequestParam(required = false) String concepto,
                            @RequestParam(defaultValue = "") String referencia, Model model) {
        return ejecutar(cajero, "deposito", model, () -> caja.depositar(cajero, cuenta, monto, concepto, referencia),
                cuenta, null, monto, concepto);
    }

    @PostMapping("/retiro")
    @PreAuthorize("hasAuthority('REALIZAR_RETIRO')")
    public String retirar(@AuthenticationPrincipal UsuarioActual cajero,
                          @RequestParam(defaultValue = "") String cuenta,
                          @RequestParam(required = false) BigDecimal monto,
                          @RequestParam(defaultValue = "false") boolean identidadVerificada,
                          @RequestParam(required = false) String concepto,
                          @RequestParam(defaultValue = "") String referencia, Model model) {
        return ejecutar(cajero, "retiro", model,
                () -> caja.retirar(cajero, cuenta, monto, identidadVerificada, concepto, referencia), cuenta, null, monto, concepto);
    }

    @PostMapping("/transferencia")
    @PreAuthorize("hasAuthority('TRANSFERIR_VENTANILLA')")
    public String transferir(@AuthenticationPrincipal UsuarioActual cajero,
                             @RequestParam(defaultValue = "") String cuenta,
                             @RequestParam(defaultValue = "") String destino,
                             @RequestParam(required = false) BigDecimal monto,
                             @RequestParam(defaultValue = "false") boolean identidadVerificada,
                             @RequestParam(required = false) String concepto,
                             @RequestParam(defaultValue = "") String referencia, Model model) {
        return ejecutar(cajero, "transferencia", model,
                () -> caja.transferir(cajero, cuenta, destino, monto, identidadVerificada, concepto, referencia),
                cuenta, destino, monto, concepto);
    }

    @GetMapping("/comprobantes/{numero}")
    @PreAuthorize("hasAuthority('ABRIR_CAJA')")
    public String comprobante(@AuthenticationPrincipal UsuarioActual cajero, @PathVariable String numero,
                              @RequestParam(required = false) String nueva, Model model) {
        Transaccion transaccion = caja.transaccionDeCajero(cajero, numero)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<MovimientoFila> filas = movimientos.deTransaccion(numero);
        if (filas.isEmpty()) {
            model.addAttribute("transaccion", transaccion);
            model.addAttribute("operacion", registro.nombreOperacion(transaccion.getIdTipoOperacion()));
            model.addAttribute("estado", registro.nombreEstado(transaccion.getIdEstadoTransaccion()));
            return "empleado/caja/pendiente";
        }
        model.addAttribute("filas", filas);
        model.addAttribute("principal", filas.get(0));
        model.addAttribute("nueva", nueva != null);
        model.addAttribute("volver", "/empleado/caja");
        return "transaccion/comprobante";
    }

    private String ejecutar(UsuarioActual cajero, String tipo, Model model,
                            java.util.function.Supplier<CajaService.ResultadoOperacion> operacion,
                            String cuenta, String destino, BigDecimal monto, String concepto) {
        try {
            CajaService.ResultadoOperacion resultado = reintento.ejecutar(operacion);
            return "redirect:/empleado/caja/comprobantes/" + resultado.numeroTransaccion() + "?nueva";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        } catch (DataAccessException e) {
            log.warn("Operación de caja rechazada", e);
            model.addAttribute("error", ErroresBaseDatos.mensajeParaUsuario(e)
                    .orElse("No se pudo completar la operación. No se movió dinero; intente de nuevo."));
        }
        model.addAttribute("cuenta", cuenta);
        model.addAttribute("destino", destino);
        model.addAttribute("monto", monto);
        model.addAttribute("concepto", concepto);
        return mostrarFormulario(cajero, tipo, model);
    }

    private String mostrarFormulario(UsuarioActual cajero, String tipo, Model model) {
        String permiso = switch (tipo) {
            case "deposito" -> "REALIZAR_DEPOSITO";
            case "retiro" -> "REALIZAR_RETIRO";
            default -> "TRANSFERIR_VENTANILLA";
        };
        if (!cajero.tienePermiso(permiso)) {
            throw new org.springframework.security.access.AccessDeniedException("Sin permiso");
        }
        model.addAttribute("tipo", tipo);
        model.addAttribute("cajaAbierta", caja.abierta(cajero).isPresent());
        model.addAttribute("referencia", CajaService.nuevaReferencia());
        model.addAttribute("limite", caja.limiteRetiro());
        return "empleado/caja/operacion";
    }
}
