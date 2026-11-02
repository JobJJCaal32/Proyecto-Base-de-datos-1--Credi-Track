package gt.umg.creditrack.cuenta;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gt.umg.creditrack.auditoria.BitacoraConsulta;
import gt.umg.creditrack.auditoria.BitacoraConsultaRepository;
import gt.umg.creditrack.auditoria.EstadoCuentaGenerado;
import gt.umg.creditrack.auditoria.EstadoCuentaGeneradoRepository;
import gt.umg.creditrack.auditoria.TipoConsulta;
import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.comun.web.Meses;
import gt.umg.creditrack.seguridad.UsuarioActual;
import gt.umg.creditrack.transaccion.ConsultaMovimientosService;
import gt.umg.creditrack.transaccion.MovimientoFila;

@Service
public class EstadoCuentaService {

    public record EstadoCuenta(EstadoCuentaGenerado registro, Cuenta cuenta, String tipoCuenta, String titular,
                               String periodo, List<MovimientoFila> movimientos) {
    }

    private final CuentaRepository cuentas;
    private final EstadoCuentaGeneradoRepository generados;
    private final BitacoraConsultaRepository bitacora;
    private final JdbcTemplate jdbc;
    private final Meses meses;

    public EstadoCuentaService(CuentaRepository cuentas, EstadoCuentaGeneradoRepository generados,
                               BitacoraConsultaRepository bitacora, JdbcTemplate jdbc, Meses meses) {
        this.cuentas = cuentas;
        this.generados = generados;
        this.bitacora = bitacora;
        this.jdbc = jdbc;
        this.meses = meses;
    }

    @Transactional
    public EstadoCuenta generar(UsuarioActual cliente, Integer idCuenta, String mes) {
        Cuenta cuenta = cuentas.findById(idCuenta == null ? -1 : idCuenta)
                .filter(c -> c.getIdCliente().equals(cliente.idCliente()))
                .orElseThrow(() -> new ReglaNegocioException("Seleccione una de sus cuentas."));
        YearMonth periodo;
        try {
            periodo = YearMonth.parse(mes);
        } catch (RuntimeException e) {
            throw new ReglaNegocioException("Seleccione un mes válido.");
        }
        Meses.Rango rango = meses.rango(mes);
        BigDecimal inicial = saldoAntesDe(cuenta.getIdCuenta(), rango.desdeUtc());
        List<MovimientoFila> movs = jdbc.query("""
                SELECT * FROM vw_movimientos
                 WHERE id_cuenta = ? AND fecha_hora >= ? AND fecha_hora < ?
                 ORDER BY fecha_hora, id_movimiento
                """, ConsultaMovimientosService.MAPEO, cuenta.getIdCuenta(),
                Timestamp.valueOf(rango.desdeUtc()), Timestamp.valueOf(rango.hastaUtc()));
        BigDecimal cargos = movs.stream().filter(MovimientoFila::esCargo).map(MovimientoFila::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal abonos = movs.stream().filter(m -> !m.esCargo()).map(MovimientoFila::monto).reduce(BigDecimal.ZERO, BigDecimal::add);

        EstadoCuentaGenerado registro = new EstadoCuentaGenerado();
        registro.setIdCuenta(cuenta.getIdCuenta());
        registro.setFechaInicio(periodo.atDay(1));
        registro.setFechaFin(periodo.atEndOfMonth());
        registro.setSaldoInicial(inicial);
        registro.setTotalCargos(cargos);
        registro.setTotalAbonos(abonos);
        registro.setSaldoFinal(inicial.add(abonos).subtract(cargos));
        registro.setCantidadMovimientos(movs.size());
        registro.setFechaGeneracion(LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
        registro.setIdUsuarioGenera(cliente.idUsuario());
        generados.save(registro);

        BitacoraConsulta consulta = new BitacoraConsulta();
        consulta.setIdSesionUsuario(cliente.idSesionUsuario());
        consulta.setUsername(cliente.username());
        consulta.setTipoConsulta(TipoConsulta.ESTADO_CUENTA);
        consulta.setIdCuenta(cuenta.getIdCuenta());
        consulta.setIdCliente(cuenta.getIdCliente());
        consulta.setCriterio("Estado de cuenta " + periodo);
        consulta.setCantidadRegistros(movs.size());
        consulta.setFechaHora(registro.getFechaGeneracion());
        bitacora.save(consulta);

        String tipo = jdbc.queryForObject("SELECT nombre FROM tipo_cuenta WHERE id_tipo_cuenta = ?", String.class, cuenta.getIdTipoCuenta());
        String titular = jdbc.queryForObject("SELECT titular FROM vw_cuentas_cliente WHERE id_cuenta = ?", String.class, cuenta.getIdCuenta());
        String etiqueta = meses.ultimos(120).stream().filter(o -> o.valor().equals(periodo.toString()))
                .map(Meses.Opcion::etiqueta).findFirst().orElse(periodo.toString());
        return new EstadoCuenta(registro, cuenta, tipo, titular, etiqueta, movs);
    }

    private BigDecimal saldoAntesDe(Integer idCuenta, LocalDateTime desdeUtc) {
        List<BigDecimal> previo = jdbc.queryForList("""
                SELECT m.saldo_posterior
                  FROM movimiento_cuenta m
                  JOIN transaccion t ON t.id_transaccion = m.id_transaccion
                 WHERE m.id_cuenta = ? AND t.fecha_hora < ?
                 ORDER BY t.fecha_hora DESC, m.id_movimiento DESC
                 LIMIT 1
                """, BigDecimal.class, idCuenta, Timestamp.valueOf(desdeUtc));
        return previo.isEmpty() ? BigDecimal.ZERO.setScale(2) : previo.get(0);
    }
}
