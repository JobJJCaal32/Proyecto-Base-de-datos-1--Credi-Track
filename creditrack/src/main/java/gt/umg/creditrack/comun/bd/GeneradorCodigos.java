package gt.umg.creditrack.comun.bd;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GeneradorCodigos {

    private static final long PRIMERA_CUENTA = 1000000001L;

    private final JdbcTemplate jdbc;

    public GeneradorCodigos(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String siguienteCodigoCliente() {
        String ultimo = ultimo("SELECT codigo_cliente FROM cliente WHERE codigo_cliente LIKE 'CLI-%' "
                + "ORDER BY codigo_cliente DESC LIMIT 1 FOR UPDATE");
        long siguiente = ultimo == null ? 1 : Long.parseLong(ultimo.substring(4)) + 1;
        return String.format("CLI-%06d", siguiente);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String siguienteCodigoEmpleado() {
        String ultimo = ultimo("SELECT codigo_empleado FROM empleado WHERE codigo_empleado LIKE 'EMP-%' "
                + "ORDER BY codigo_empleado DESC LIMIT 1 FOR UPDATE");
        long siguiente = ultimo == null ? 1 : Long.parseLong(ultimo.substring(4)) + 1;
        return String.format("EMP-%04d", siguiente);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String siguienteNumeroCuenta() {
        String ultimo = ultimo("SELECT numero_cuenta FROM cuenta ORDER BY numero_cuenta DESC LIMIT 1 FOR UPDATE");
        long siguiente = ultimo == null ? PRIMERA_CUENTA : Long.parseLong(ultimo) + 1;
        return String.valueOf(siguiente);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String siguienteNumeroTransaccion() {
        String ultimo = ultimo("SELECT numero_transaccion FROM transaccion WHERE numero_transaccion LIKE 'TRX-%' "
                + "ORDER BY numero_transaccion DESC LIMIT 1 FOR UPDATE");
        long siguiente = ultimo == null ? 1 : Long.parseLong(ultimo.substring(4)) + 1;
        return String.format("TRX-%010d", siguiente);
    }

    private String ultimo(String sql) {
        List<String> filas = jdbc.queryForList(sql, String.class);
        return filas.isEmpty() ? null : filas.get(0);
    }
}
