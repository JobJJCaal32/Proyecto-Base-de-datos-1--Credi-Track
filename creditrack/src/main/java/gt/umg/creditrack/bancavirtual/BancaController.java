package gt.umg.creditrack.bancavirtual;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

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
import gt.umg.creditrack.comun.web.Meses;
import gt.umg.creditrack.cuenta.AliasCuentaService;
import gt.umg.creditrack.cuenta.ConsultaCuentasService;
import gt.umg.creditrack.cuenta.CuentaClienteFila;
import gt.umg.creditrack.seguridad.UsuarioActual;
import gt.umg.creditrack.transaccion.ConsultaMovimientosService;
import gt.umg.creditrack.transaccion.MovimientoFila;
import gt.umg.creditrack.transaccion.TransferenciaService;

@Controller
@RequestMapping("/banca")
public class BancaController {

    private static final Logger log = LoggerFactory.getLogger(BancaController.class);
    private static final Set<String> TIPOS_OPERACION = Set.of("DEPOSITO", "RETIRO", "TRANSFERENCIA");

    private final ConsultaCuentasService consultaCuentas;
    private final AliasCuentaService alias;
    private final TransferenciaService transferencias;
    private final ConsultaMovimientosService movimientos;
    private final ReintentoTransaccional reintento;
    private final Meses meses;

    public BancaController(ConsultaCuentasService consultaCuentas, AliasCuentaService alias, TransferenciaService transferencias,
                           ConsultaMovimientosService movimientos, ReintentoTransaccional reintento, Meses meses) {
        this.consultaCuentas = consultaCuentas;
        this.alias = alias;
        this.transferencias = transferencias;
        this.movimientos = movimientos;
        this.reintento = reintento;
        this.meses = meses;
    }

    @GetMapping("/cuentas")
    @PreAuthorize("hasAuthority('CONSULTAR_CUENTAS')")
    public String cuentas(@AuthenticationPrincipal UsuarioActual cliente,
                          @RequestParam(name = "alias-guardado", required = false) String aliasGuardado,
                          @RequestParam(name = "alias-invalido", required = false) String aliasInvalido, Model model) {
        if (aliasGuardado != null) {
            model.addAttribute("aviso", "El alias se guardó.");
        }
        if (aliasInvalido != null) {
            model.addAttribute("error", "El alias admite hasta 50 letras, números, espacios y . , ' -");
        }
        List<CuentaClienteFila> lista = consultaCuentas.deCliente(cliente.idSesionUsuario(), cliente.idCliente());
        model.addAttribute("cuentas", lista);
        model.addAttribute("total", lista.stream().map(CuentaClienteFila::saldoDisponible).reduce(BigDecimal.ZERO, BigDecimal::add));
        return "banca/cuentas";
    }

    @PostMapping("/cuentas/{numero}/alias")
    @PreAuthorize("hasAuthority('GESTIONAR_ALIAS')")
    public String cambiarAlias(@AuthenticationPrincipal UsuarioActual cliente, @PathVariable String numero,
                               @RequestParam(required = false) String alias) {
        try {
            this.alias.cambiar(cliente, numero, alias);
            return "redirect:/banca/cuentas?alias-guardado";
        } catch (ReglaNegocioException e) {
            return "redirect:/banca/cuentas?alias-invalido";
        }
    }

    @GetMapping("/transferir")
    @PreAuthorize("hasAuthority('TRANSFERIR')")
    public String transferir(@AuthenticationPrincipal UsuarioActual cliente,
                             @RequestParam(required = false) Integer origen, Model model) {
        model.addAttribute("origen", origen);
        return formularioTransferencia(cliente, model);
    }

    @PostMapping("/transferir/confirmar")
    @PreAuthorize("hasAuthority('TRANSFERIR')")
    public String confirmar(@AuthenticationPrincipal UsuarioActual cliente,
                            @RequestParam(required = false) Integer origen,
                            @RequestParam(defaultValue = "") String destino,
                            @RequestParam(required = false) BigDecimal monto,
                            @RequestParam(required = false) String concepto, Model model) {
        try {
            model.addAttribute("vista", transferencias.preparar(cliente, origen, destino, monto, concepto));
            return "banca/confirmar";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("origen", origen);
            model.addAttribute("destino", destino);
            model.addAttribute("monto", monto);
            model.addAttribute("concepto", concepto);
            return formularioTransferencia(cliente, model);
        }
    }

    @PostMapping("/transferir")
    @PreAuthorize("hasAuthority('TRANSFERIR')")
    public String ejecutar(@AuthenticationPrincipal UsuarioActual cliente,
                           @RequestParam(required = false) Integer origen,
                           @RequestParam(defaultValue = "") String destino,
                           @RequestParam(required = false) BigDecimal monto,
                           @RequestParam(required = false) String concepto,
                           @RequestParam(defaultValue = "") String referencia,
                           @RequestParam(required = false) String claveOperacion, Model model) {
        try {
            String numero = reintento.ejecutar(() ->
                    transferencias.transferir(cliente, origen, destino, monto, concepto, referencia, claveOperacion));
            return "redirect:/banca/comprobantes/" + numero + "?nueva";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        } catch (DataAccessException e) {
            log.warn("No se pudo completar la transferencia", e);
            model.addAttribute("error", ErroresBaseDatos.mensajeParaUsuario(e)
                    .orElse("No se pudo completar la transferencia. No se movió dinero; intente de nuevo."));
        }
        model.addAttribute("origen", origen);
        model.addAttribute("destino", destino);
        model.addAttribute("monto", monto);
        model.addAttribute("concepto", concepto);
        return formularioTransferencia(cliente, model);
    }

    @GetMapping("/movimientos")
    @PreAuthorize("hasAuthority('CONSULTAR_MOVIMIENTOS')")
    public String movimientos(@AuthenticationPrincipal UsuarioActual cliente,
                              @RequestParam(required = false) Integer cuenta,
                              @RequestParam(required = false) String mes,
                              @RequestParam(required = false) String tipo, Model model) {
        String mesElegido = mes == null ? meses.ultimos(1).get(0).valor() : mes;
        String tipoElegido = tipo != null && TIPOS_OPERACION.contains(tipo) ? tipo : null;
        Meses.Rango rango = meses.rango(mesElegido);
        model.addAttribute("cuentas", transferencias.cuentasDelCliente(cliente));
        model.addAttribute("meses", meses.ultimos(12));
        model.addAttribute("cuenta", cuenta);
        model.addAttribute("mes", mesElegido);
        model.addAttribute("tipo", tipoElegido);
        try {
            model.addAttribute("movimientos", movimientos.buscar(cliente.idSesionUsuario(), cliente.idCliente(), cuenta,
                    rango.desdeUtc(), rango.hastaUtc(), tipoElegido));
        } catch (DataAccessException e) {
            model.addAttribute("movimientos", List.of());
            model.addAttribute("error", ErroresBaseDatos.mensajeParaUsuario(e).orElse("No se pudieron consultar los movimientos."));
        }
        return "banca/movimientos";
    }

    @GetMapping("/comprobantes/{numero}")
    @PreAuthorize("hasAuthority('CONSULTAR_MOVIMIENTOS')")
    public String comprobante(@AuthenticationPrincipal UsuarioActual cliente, @PathVariable String numero,
                              @RequestParam(required = false) String nueva, Model model) {
        List<MovimientoFila> filas = movimientos.deTransaccion(numero);
        if (filas.isEmpty() || filas.stream().noneMatch(f -> f.idCliente().equals(cliente.idCliente()))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("filas", filas);
        model.addAttribute("principal", filas.get(0));
        model.addAttribute("nueva", nueva != null);
        model.addAttribute("volver", "/banca/movimientos");
        return "transaccion/comprobante";
    }

    private String formularioTransferencia(UsuarioActual cliente, Model model) {
        model.addAttribute("cuentasOrigen", transferencias.cuentasOrigen(cliente));
        model.addAttribute("requiereClave", transferencias.tieneClaveOperacion(cliente));
        return "banca/transferir";
    }
}
