package gt.umg.creditrack.auditoria;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gt.umg.creditrack.comun.web.Meses;

@Controller
@RequestMapping("/empleado/auditoria")
public class AuditoriaController {

    private static final Map<String, String> TIPOS_CONSULTA = tiposConsulta();

    private final ConsultaAuditoriaService consulta;
    private final ConsistenciaService consistencia;
    private final Meses meses;

    public AuditoriaController(ConsultaAuditoriaService consulta, ConsistenciaService consistencia, Meses meses) {
        this.consulta = consulta;
        this.consistencia = consistencia;
        this.meses = meses;
    }

    @GetMapping("/bitacora")
    @PreAuthorize("hasAuthority('CONSULTAR_BITACORA')")
    public String bitacora(@RequestParam(required = false) String tabla,
                           @RequestParam(required = false) String usuario,
                           @RequestParam(required = false) String accion,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                           @RequestParam(defaultValue = "1") int pagina, Model model) {
        Meses.Rango rango = meses.rangoDias(desde, hasta);
        model.addAttribute("tablas", consulta.tablasAuditadas());
        model.addAttribute("pagina", consulta.bitacora(tabla, usuario, accion, rango.desdeUtc(), rango.hastaUtc(), pagina));
        model.addAttribute("tabla", tabla);
        model.addAttribute("usuario", usuario);
        model.addAttribute("accion", accion);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        return "empleado/auditoria/bitacora";
    }

    @GetMapping("/intentos")
    @PreAuthorize("hasAuthority('CONSULTAR_INTENTOS_ACCESO')")
    public String intentos(@RequestParam(required = false) String usuario,
                           @RequestParam(required = false) String ip,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                           @RequestParam(defaultValue = "1") int pagina, Model model) {
        Meses.Rango rango = meses.rangoDias(desde, hasta);
        model.addAttribute("pagina", consulta.intentos(usuario, ip, rango.desdeUtc(), rango.hastaUtc(), pagina));
        model.addAttribute("porUsuario", consulta.resumenIntentos("usuario", rango.desdeUtc(), rango.hastaUtc()));
        model.addAttribute("porIp", consulta.resumenIntentos("ip", rango.desdeUtc(), rango.hastaUtc()));
        model.addAttribute("usuario", usuario);
        model.addAttribute("ip", ip);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        return "empleado/auditoria/intentos";
    }

    @GetMapping("/consultas")
    @PreAuthorize("hasAuthority('CONSULTAR_BITACORA')")
    public String consultas(@RequestParam(required = false) String usuario,
                            @RequestParam(required = false) String cliente,
                            @RequestParam(required = false) String tipo,
                            @RequestParam(required = false) String quien,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                            @RequestParam(defaultValue = "1") int pagina, Model model) {
        Meses.Rango rango = meses.rangoDias(desde, hasta);
        String tipoValido = tipo != null && TIPOS_CONSULTA.containsKey(tipo) ? tipo : null;
        String quienValido = "EMPLEADO".equals(quien) || "CLIENTE".equals(quien) ? quien : null;
        model.addAttribute("tipos", TIPOS_CONSULTA);
        model.addAttribute("pagina", consulta.consultas(usuario, cliente, tipoValido, quienValido, rango.desdeUtc(), rango.hastaUtc(), pagina));
        model.addAttribute("porUsuario", consulta.resumenConsultas("usuario", usuario, cliente, tipoValido, quienValido,
                rango.desdeUtc(), rango.hastaUtc()));
        model.addAttribute("porCliente", consulta.resumenConsultas("cliente", usuario, cliente, tipoValido, quienValido,
                rango.desdeUtc(), rango.hastaUtc()));
        model.addAttribute("usuario", usuario);
        model.addAttribute("cliente", cliente);
        model.addAttribute("tipo", tipoValido);
        model.addAttribute("quien", quienValido);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        return "empleado/auditoria/consultas";
    }

    @GetMapping("/cajas")
    @PreAuthorize("hasAuthority('CONSULTAR_REPORTES')")
    public String cajas(@RequestParam(required = false) Integer sucursal,
                        @RequestParam(required = false) String cajero,
                        @RequestParam(required = false) String resultado,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                        @RequestParam(defaultValue = "1") int pagina, Model model) {
        Meses.Rango rango = meses.rangoDias(desde, hasta);
        model.addAttribute("sucursales", consulta.sucursales());
        model.addAttribute("pagina", consulta.cajas(sucursal, cajero, resultado, rango.desdeUtc(), rango.hastaUtc(), pagina));
        model.addAttribute("totales", consulta.totalesCajas(sucursal, cajero, rango.desdeUtc(), rango.hastaUtc()));
        model.addAttribute("sucursal", sucursal);
        model.addAttribute("cajero", cajero);
        model.addAttribute("resultado", resultado);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        return "empleado/auditoria/cajas";
    }

    @GetMapping("/consistencia")
    @PreAuthorize("hasAuthority('CONSULTAR_REPORTES')")
    public String consistencia(Model model) {
        model.addAttribute("resultado", consistencia.verificar());
        return "empleado/auditoria/consistencia";
    }

    private static Map<String, String> tiposConsulta() {
        Map<String, String> tipos = new LinkedHashMap<>();
        tipos.put(TipoConsulta.BUSQUEDA_DPI.name(), "Búsqueda por DPI");
        tipos.put(TipoConsulta.DATO_PERSONAL.name(), "Datos personales");
        tipos.put(TipoConsulta.SALDO.name(), "Saldos");
        tipos.put(TipoConsulta.MOVIMIENTOS.name(), "Movimientos");
        tipos.put(TipoConsulta.ESTADO_CUENTA.name(), "Estado de cuenta");
        return Collections.unmodifiableMap(tipos);
    }
}
