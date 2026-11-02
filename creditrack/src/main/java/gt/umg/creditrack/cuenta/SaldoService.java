package gt.umg.creditrack.cuenta;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.transaccion.MovimientoCuenta;
import gt.umg.creditrack.transaccion.MovimientoCuentaRepository;
import gt.umg.creditrack.transaccion.Naturaleza;

@Service
public class SaldoService {

    private static final String SQL_BLOQUEAR = """
            SELECT c.numero_cuenta, c.saldo_actual, c.saldo_retenido, ec.permite_operar, ec.nombre AS estado
              FROM cuenta c
              JOIN estado_cuenta ec ON ec.id_estado_cuenta = c.id_estado_cuenta
             WHERE c.id_cuenta = ?
               FOR UPDATE OF c
            """;

    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;
    private final MovimientoCuentaRepository movimientos;

    public SaldoService(JdbcTemplate jdbc, EntityManager entityManager, MovimientoCuentaRepository movimientos) {
        this.jdbc = jdbc;
        this.entityManager = entityManager;
        this.movimientos = movimientos;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void bloquearEnOrden(Collection<Integer> idsCuenta) {
        entityManager.flush();
        String lista = idsCuenta.stream().sorted().distinct().map(String::valueOf).collect(Collectors.joining(","));
        if (!lista.isEmpty()) {
            jdbc.queryForList("SELECT id_cuenta FROM cuenta WHERE id_cuenta IN (" + lista + ") ORDER BY id_cuenta FOR UPDATE", Integer.class);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public MovimientoCuenta registrarMovimiento(Long idTransaccion, Integer idCuenta, Naturaleza naturaleza,
                                                BigDecimal monto, int orden) {
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaNegocioException("El monto debe ser mayor que cero.");
        }
        entityManager.flush();
        Map<String, Object> cuenta = jdbc.queryForMap(SQL_BLOQUEAR, idCuenta);
        String numeroCuenta = (String) cuenta.get("numero_cuenta");
        if (!verdadero(cuenta.get("permite_operar"))) {
            throw new ReglaNegocioException("La cuenta " + numeroCuenta + " está " + cuenta.get("estado") + " y no permite operar.");
        }
        BigDecimal anterior = (BigDecimal) cuenta.get("saldo_actual");
        BigDecimal retenido = (BigDecimal) cuenta.get("saldo_retenido");
        BigDecimal posterior;
        if (naturaleza == Naturaleza.CARGO) {
            if (anterior.subtract(retenido).compareTo(monto) < 0) {
                throw new ReglaNegocioException("La cuenta " + numeroCuenta + " no tiene saldo disponible suficiente.");
            }
            posterior = anterior.subtract(monto);
        } else {
            posterior = anterior.add(monto);
        }

        MovimientoCuenta movimiento = new MovimientoCuenta();
        movimiento.setIdTransaccion(idTransaccion);
        movimiento.setIdCuenta(idCuenta);
        movimiento.setNaturaleza(naturaleza);
        movimiento.setMonto(monto);
        movimiento.setSaldoAnterior(anterior);
        movimiento.setSaldoPosterior(posterior);
        movimiento.setOrden(orden);
        movimientos.save(movimiento);
        entityManager.flush();

        jdbc.execute("SET @permitir_cambio_saldo = 1");
        try {
            jdbc.update("UPDATE cuenta SET saldo_actual = ? WHERE id_cuenta = ?", posterior, idCuenta);
        } finally {
            jdbc.execute("SET @permitir_cambio_saldo = 0");
        }
        return movimiento;
    }

    private static boolean verdadero(Object valor) {
        if (valor instanceof Boolean b) {
            return b;
        }
        return valor instanceof Number n && n.intValue() == 1;
    }
}
