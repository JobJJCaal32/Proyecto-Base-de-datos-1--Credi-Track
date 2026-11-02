package gt.umg.creditrack.supervision;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gt.umg.creditrack.caja.CajaService;
import gt.umg.creditrack.caja.EstadoCaja;
import gt.umg.creditrack.caja.SesionCaja;
import gt.umg.creditrack.caja.SesionCajaRepository;
import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.cuenta.CuentaRepository;
import gt.umg.creditrack.cuenta.SaldoService;
import gt.umg.creditrack.seguridad.UsuarioActual;
import gt.umg.creditrack.transaccion.Canal;
import gt.umg.creditrack.transaccion.MovimientoCuenta;
import gt.umg.creditrack.transaccion.MovimientoCuentaRepository;
import gt.umg.creditrack.transaccion.Naturaleza;
import gt.umg.creditrack.transaccion.RegistroTransacciones;
import gt.umg.creditrack.transaccion.Transaccion;
import gt.umg.creditrack.transaccion.TransaccionRepository;

@Service
public class SupervisionService {

    public record Pendiente(String numero, LocalDateTime fechaHora, String operacion, BigDecimal monto, String cuenta,
                            String titular, String cajero, String sucursal) {
    }

    public record Reversible(String numero, LocalDateTime fechaHora, BigDecimal monto, String canal, String origen,
                             String destino, String concepto, String realizadoPor) {
    }

    public record FilaAlerta(Long id, LocalDateTime fecha, String tipo, String nivel, String detalle, String estado,
                             String numeroTransaccion, String cuenta, String revisor, String resolucion) {
    }

    public record FilaCaja(Integer id, String cajero, String sucursal, LocalDateTime apertura, LocalDateTime cierre,
                           BigDecimal montoApertura, BigDecimal calculado, BigDecimal declarado, BigDecimal diferencia,
                           Integer transacciones, String estado, String observaciones) {
    }

    private static final String SQL_PENDIENTES = """
            SELECT t.numero_transaccion, t.fecha_hora, op.nombre AS operacion, t.monto,
                   CONCAT_WS(' ', ie.primer_nombre, ie.primer_apellido) AS cajero, s.nombre AS sucursal
              FROM transaccion t
              JOIN tipo_operacion op ON op.id_tipo_operacion = t.id_tipo_operacion
              JOIN estado_transaccion et ON et.id_estado_transaccion = t.id_estado_transaccion
              JOIN sesion_caja sc ON sc.id_sesion_caja = t.id_sesion_caja
              JOIN empleado e ON e.id_empleado = sc.id_empleado_cajero
              JOIN identidad ie ON ie.id_identidad = e.id_identidad
              JOIN sucursal s ON s.id_sucursal = sc.id_sucursal
             WHERE et.nombre = 'PENDIENTE'
             ORDER BY t.fecha_hora
            """;

    private final JdbcTemplate jdbc;
    private final TransaccionRepository transacciones;
    private final MovimientoCuentaRepository movimientos;
    private final SesionCajaRepository sesionesCaja;
    private final CuentaRepository cuentas;
    private final AutorizacionRepository autorizaciones;
    private final AlertaRepository alertas;
    private final RegistroTransacciones registro;
    private final SaldoService saldos;
    private final CajaService caja;
    private final AlertaService alertaService;

    public SupervisionService(JdbcTemplate jdbc, TransaccionRepository transacciones, MovimientoCuentaRepository movimientos,
                              SesionCajaRepository sesionesCaja, CuentaRepository cuentas,
                              AutorizacionRepository autorizaciones, AlertaRepository alertas,
                              RegistroTransacciones registro, SaldoService saldos, CajaService caja, AlertaService alertaService) {
        this.jdbc = jdbc;
        this.transacciones = transacciones;
        this.movimientos = movimientos;
        this.sesionesCaja = sesionesCaja;
        this.cuentas = cuentas;
        this.autorizaciones = autorizaciones;
        this.alertas = alertas;
        this.registro = registro;
        this.saldos = saldos;
        this.caja = caja;
        this.alertaService = alertaService;
    }

    public List<Pendiente> pendientes() {
        return jdbc.query(SQL_PENDIENTES, (f, n) -> {
            String numero = f.getString("numero_transaccion");
            String[] cuenta = cuentaDePendiente(numero);
            return new Pendiente(numero, f.getTimestamp("fecha_hora").toLocalDateTime(), f.getString("operacion"),
                    f.getBigDecimal("monto"), cuenta[0], cuenta[1], f.getString("cajero"), f.getString("sucursal"));
        });
    }

    @Transactional
    public void resolverPendiente(UsuarioActual supervisor, String numero, boolean aprobar, String comentario) {
        Transaccion t = transacciones.findByNumeroTransaccion(numero)
                .orElseThrow(() -> new ReglaNegocioException("La transacción no existe."));
        if (!RegistroTransacciones.PENDIENTE.equals(registro.nombreEstado(t.getIdEstadoTransaccion()))) {
            throw new ReglaNegocioException("La transacción " + numero + " ya fue resuelta.");
        }
        SesionCaja sesion = sesionesCaja.findById(t.getIdSesionCaja()).orElseThrow();
        if (sesion.getIdEmpleadoCajero().equals(supervisor.idEmpleado())) {
            throw new ReglaNegocioException("No puede autorizar una operación de su propia caja.");
        }
        if (aprobar) {
            if (sesion.getEstado() != EstadoCaja.ABIERTA) {
                throw new ReglaNegocioException("La caja de esa operación ya está cerrada; solo puede rechazarla.");
            }
            String numeroCuenta = cuentaDePendiente(numero)[0];
            Integer idCuenta = cuentas.findByNumeroCuenta(numeroCuenta).orElseThrow().getIdCuenta();
            if (caja.resumen(sesion).efectivoEsperado().compareTo(t.getMonto()) < 0) {
                throw new ReglaNegocioException("La caja no tiene efectivo suficiente para este retiro.");
            }
            saldos.registrarMovimiento(t.getIdTransaccion(), idCuenta, Naturaleza.CARGO, t.getMonto(), 1);
            alertaService.evaluarCargo(t.getIdTransaccion(), idCuenta, numeroCuenta, t.getMonto(), "Retiro autorizado");
            sesion.setCantidadTransacciones(sesion.getCantidadTransacciones() + 1);
        }
        t.setIdEstadoTransaccion(registro.idEstado(aprobar ? RegistroTransacciones.APLICADA : RegistroTransacciones.RECHAZADA));
        guardarAutorizacion(t.getIdTransaccion(), MotivoAutorizacion.SOBRE_LIMITE, supervisor,
                aprobar ? ResultadoAutorizacion.APROBADA : ResultadoAutorizacion.RECHAZADA, comentario);
    }

    public List<Reversible> reversibles(String numero) {
        String sql = """
                SELECT t.numero_transaccion, t.fecha_hora, t.monto, t.canal, t.concepto,
                       mo.numero_cuenta AS origen, md.numero_cuenta AS destino, u.username
                  FROM transaccion t
                  JOIN tipo_operacion op ON op.id_tipo_operacion = t.id_tipo_operacion AND op.nombre = 'TRANSFERENCIA'
                  JOIN estado_transaccion et ON et.id_estado_transaccion = t.id_estado_transaccion AND et.nombre = 'APLICADA'
                  JOIN vw_movimientos mo ON mo.id_transaccion = t.id_transaccion AND mo.naturaleza = 'CARGO'
                  JOIN vw_movimientos md ON md.id_transaccion = t.id_transaccion AND md.naturaleza = 'ABONO'
                  JOIN sesion_usuario su ON su.id_sesion_usuario = t.id_sesion_usuario
                  JOIN usuario u ON u.id_usuario = su.id_usuario
                 WHERE t.id_transaccion_origen IS NULL
                """ + (numero == null || numero.isBlank() ? "" : " AND t.numero_transaccion = ?")
                + " ORDER BY t.fecha_hora DESC LIMIT 50";
        Object[] parametros = numero == null || numero.isBlank() ? new Object[0] : new Object[] {numero.trim().toUpperCase()};
        return jdbc.query(sql, (f, n) -> new Reversible(f.getString("numero_transaccion"),
                f.getTimestamp("fecha_hora").toLocalDateTime(), f.getBigDecimal("monto"), f.getString("canal"),
                f.getString("origen"), f.getString("destino"), f.getString("concepto"), f.getString("username")), parametros);
    }

    @Transactional
    public String reversar(UsuarioActual supervisor, String numero, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("Escriba el motivo del reverso.");
        }
        Transaccion original = transacciones.findByNumeroTransaccion(numero)
                .orElseThrow(() -> new ReglaNegocioException("La transacción no existe."));
        if (!"TRANSFERENCIA".equals(registro.nombreOperacion(original.getIdTipoOperacion()))
                || !RegistroTransacciones.APLICADA.equals(registro.nombreEstado(original.getIdEstadoTransaccion()))
                || original.getIdTransaccionOrigen() != null) {
            throw new ReglaNegocioException("Solo se reversan transferencias aplicadas que no sean a su vez un reverso.");
        }
        List<MovimientoCuenta> movs = movimientos.findByIdTransaccionOrderByOrden(original.getIdTransaccion());
        MovimientoCuenta cargo = movs.stream().filter(m -> m.getNaturaleza() == Naturaleza.CARGO).findFirst().orElseThrow();
        MovimientoCuenta abono = movs.stream().filter(m -> m.getNaturaleza() == Naturaleza.ABONO).findFirst().orElseThrow();
        saldos.bloquearEnOrden(List.of(cargo.getIdCuenta(), abono.getIdCuenta()));
        Transaccion reverso = registro.nueva("TRANSFERENCIA", RegistroTransacciones.APLICADA, original.getMonto(), Canal.VIRTUAL,
                supervisor.idSesionUsuario(), null, recortar("Reverso de " + numero + ": " + motivo.trim(), 200),
                "REV-" + numero, false, original.getIdTransaccion());
        saldos.registrarMovimiento(reverso.getIdTransaccion(), abono.getIdCuenta(), Naturaleza.CARGO, original.getMonto(), 1);
        saldos.registrarMovimiento(reverso.getIdTransaccion(), cargo.getIdCuenta(), Naturaleza.ABONO, original.getMonto(), 2);
        original.setIdEstadoTransaccion(registro.idEstado(RegistroTransacciones.REVERSADA));
        guardarAutorizacion(original.getIdTransaccion(), MotivoAutorizacion.REVERSO, supervisor, ResultadoAutorizacion.APROBADA, motivo);
        return reverso.getNumeroTransaccion();
    }

    public List<FilaAlerta> alertas(String estado) {
        String sql = """
                SELECT a.id_alerta, a.fecha_deteccion, ta.nombre AS tipo, ta.nivel_riesgo, a.detalle, a.estado,
                       t.numero_transaccion, c.numero_cuenta, CONCAT_WS(' ', ir.primer_nombre, ir.primer_apellido) AS revisor,
                       a.resolucion
                  FROM alerta a
                  JOIN tipo_alerta ta ON ta.id_tipo_alerta = a.id_tipo_alerta
                  JOIN transaccion t ON t.id_transaccion = a.id_transaccion
                  JOIN cuenta c ON c.id_cuenta = a.id_cuenta
                  LEFT JOIN empleado er ON er.id_empleado = a.id_empleado_revisor
                  LEFT JOIN identidad ir ON ir.id_identidad = er.id_identidad
                """ + (estado == null || estado.isBlank() ? " WHERE a.estado IN ('PENDIENTE', 'EN_REVISION')" : " WHERE a.estado = ?")
                + " ORDER BY a.fecha_deteccion DESC LIMIT 100";
        Object[] parametros = estado == null || estado.isBlank() ? new Object[0] : new Object[] {estado};
        return jdbc.query(sql, (f, n) -> new FilaAlerta(f.getLong("id_alerta"), f.getTimestamp("fecha_deteccion").toLocalDateTime(),
                f.getString("tipo"), f.getString("nivel_riesgo"), f.getString("detalle"), f.getString("estado"),
                f.getString("numero_transaccion"), f.getString("numero_cuenta"), f.getString("revisor"), f.getString("resolucion")),
                parametros);
    }

    @Transactional
    public void gestionarAlerta(UsuarioActual supervisor, Long idAlerta, EstadoAlerta nuevo, String resolucion) {
        Alerta alerta = alertas.findById(idAlerta).orElseThrow(() -> new ReglaNegocioException("La alerta no existe."));
        if (alerta.getEstado() == EstadoAlerta.RESUELTA || alerta.getEstado() == EstadoAlerta.DESCARTADA) {
            throw new ReglaNegocioException("La alerta ya está cerrada.");
        }
        if (nuevo != EstadoAlerta.EN_REVISION && (resolucion == null || resolucion.isBlank())) {
            throw new ReglaNegocioException("Escriba la resolución de la alerta.");
        }
        alerta.setEstado(nuevo);
        alerta.setIdEmpleadoRevisor(supervisor.idEmpleado());
        alerta.setFechaRevision(ahora());
        if (resolucion != null && !resolucion.isBlank()) {
            alerta.setResolucion(recortar(resolucion.trim(), 300));
        }
    }

    public List<FilaCaja> cajas(String estado) {
        String sql = """
                SELECT sc.id_sesion_caja, CONCAT_WS(' ', i.primer_nombre, i.primer_apellido) AS cajero, s.nombre AS sucursal,
                       sc.fecha_apertura, sc.fecha_cierre, sc.monto_apertura, sc.monto_cierre_calculado, sc.monto_cierre_declarado,
                       sc.diferencia, sc.cantidad_transacciones, sc.estado, sc.observaciones
                  FROM sesion_caja sc
                  JOIN empleado e ON e.id_empleado = sc.id_empleado_cajero
                  JOIN identidad i ON i.id_identidad = e.id_identidad
                  JOIN sucursal s ON s.id_sucursal = sc.id_sucursal
                """ + (estado == null || estado.isBlank() ? "" : " WHERE sc.estado = ?") + " ORDER BY sc.fecha_apertura DESC LIMIT 100";
        Object[] parametros = estado == null || estado.isBlank() ? new Object[0] : new Object[] {estado};
        return jdbc.query(sql, (f, n) -> new FilaCaja(f.getInt("id_sesion_caja"), f.getString("cajero"), f.getString("sucursal"),
                fecha(f.getTimestamp("fecha_apertura")), fecha(f.getTimestamp("fecha_cierre")), f.getBigDecimal("monto_apertura"),
                f.getBigDecimal("monto_cierre_calculado"), f.getBigDecimal("monto_cierre_declarado"), f.getBigDecimal("diferencia"),
                f.getInt("cantidad_transacciones"), f.getString("estado"), f.getString("observaciones")), parametros);
    }

    @Transactional
    public void arquear(UsuarioActual supervisor, Integer idSesionCaja, String observaciones) {
        SesionCaja sesion = sesionesCaja.findById(idSesionCaja == null ? -1 : idSesionCaja)
                .orElseThrow(() -> new ReglaNegocioException("La caja no existe."));
        if (sesion.getEstado() != EstadoCaja.CERRADA) {
            throw new ReglaNegocioException("Solo se arquea una caja cerrada.");
        }
        if (sesion.getIdEmpleadoCajero().equals(supervisor.idEmpleado())) {
            throw new ReglaNegocioException("No puede arquear su propia caja.");
        }
        sesion.setEstado(EstadoCaja.ARQUEADA);
        sesion.setIdEmpleadoSupervisor(supervisor.idEmpleado());
        sesion.setFechaArqueo(ahora());
        String nota = observaciones == null || observaciones.isBlank() ? null : observaciones.trim();
        if (nota != null) {
            String previas = sesion.getObservaciones();
            sesion.setObservaciones(recortar(previas == null ? "Arqueo: " + nota : previas + " | Arqueo: " + nota, 300));
        }
    }

    private String[] cuentaDePendiente(String numero) {
        String referencia = transacciones.findByNumeroTransaccion(numero).map(Transaccion::getReferencia).orElse("");
        String numeroCuenta = CajaService.cuentaDeReferencia(referencia);
        List<String> titular = jdbc.queryForList("""
                SELECT CONCAT_WS(' ', i.primer_nombre, i.primer_apellido)
                  FROM cuenta c
                  JOIN cliente cl ON cl.id_cliente = c.id_cliente
                  JOIN identidad i ON i.id_identidad = cl.id_identidad
                 WHERE c.numero_cuenta = ?
                """, String.class, numeroCuenta);
        return new String[] {numeroCuenta, titular.isEmpty() ? "" : titular.get(0)};
    }

    private void guardarAutorizacion(Long idTransaccion, MotivoAutorizacion motivo, UsuarioActual supervisor,
                                     ResultadoAutorizacion resultado, String comentario) {
        Autorizacion a = new Autorizacion();
        a.setIdTransaccion(idTransaccion);
        a.setMotivo(motivo);
        a.setIdEmpleadoSupervisor(supervisor.idEmpleado());
        a.setFechaResolucion(ahora());
        a.setResultado(resultado);
        a.setComentario(comentario == null || comentario.isBlank() ? null : recortar(comentario.trim(), 300));
        autorizaciones.save(a);
    }

    private static LocalDateTime fecha(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() > maximo ? texto.substring(0, maximo) : texto;
    }

    private static LocalDateTime ahora() {
        return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }
}
