package gt.umg.creditrack.transaccion;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class ConsultaMovimientosService {

    public static final RowMapper<MovimientoFila> MAPEO = ConsultaMovimientosService::mapear;

    private final JdbcTemplate jdbc;

    public ConsultaMovimientosService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<MovimientoFila> buscar(Long idSesionUsuario, Integer idCliente, Integer idCuenta,
                                       LocalDateTime desdeUtc, LocalDateTime hastaUtc, String tipoOperacion) {
        return jdbc.query("CALL sp_movimientos(?, ?, ?, ?, ?, ?)", MAPEO, idSesionUsuario, idCliente, idCuenta,
                desdeUtc == null ? null : Timestamp.valueOf(desdeUtc),
                hastaUtc == null ? null : Timestamp.valueOf(hastaUtc),
                tipoOperacion == null || tipoOperacion.isBlank() ? null : tipoOperacion);
    }

    public List<MovimientoFila> deTransaccion(String numeroTransaccion) {
        return jdbc.query("SELECT * FROM vw_movimientos WHERE numero_transaccion = ? ORDER BY orden", MAPEO, numeroTransaccion);
    }

    private static MovimientoFila mapear(ResultSet fila, int numero) throws SQLException {
        Timestamp fecha = fila.getTimestamp("fecha_hora");
        return new MovimientoFila(
                fila.getLong("id_movimiento"),
                fila.getLong("id_transaccion"),
                fila.getString("numero_transaccion"),
                fecha == null ? null : fecha.toLocalDateTime(),
                fila.getString("tipo_operacion"),
                fila.getString("estado_transaccion"),
                fila.getString("canal"),
                fila.getString("concepto"),
                fila.getString("referencia"),
                fila.getInt("id_cuenta"),
                fila.getString("numero_cuenta"),
                fila.getInt("id_cliente"),
                fila.getString("codigo_cliente"),
                fila.getString("titular"),
                fila.getString("naturaleza"),
                fila.getBigDecimal("monto"),
                fila.getBigDecimal("saldo_anterior"),
                fila.getBigDecimal("saldo_posterior"),
                fila.getInt("orden"),
                fila.getString("cuenta_contraparte"),
                fila.getString("titular_contraparte"),
                fila.getString("realizado_por"));
    }
}
