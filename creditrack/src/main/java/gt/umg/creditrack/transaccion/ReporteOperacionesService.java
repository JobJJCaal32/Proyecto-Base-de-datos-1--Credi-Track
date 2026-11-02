package gt.umg.creditrack.transaccion;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReporteOperacionesService {

    public record Operacion(String numeroTransaccion, LocalDateTime fechaHora, String tipoOperacion, String canal,
                            String estado, BigDecimal monto, String sucursal, String empleado, String realizadoPor) {
    }

    public record Resumen(String sucursal, String empleado, String tipoOperacion, long cantidad, BigDecimal total) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public ReporteOperacionesService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<String> sucursales() {
        return jdbc.getJdbcTemplate().queryForList("SELECT DISTINCT sucursal FROM vw_operaciones_sucursal ORDER BY sucursal", String.class);
    }

    public List<Operacion> operaciones(String sucursal, String canal, LocalDateTime desde, LocalDateTime hasta) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String sql = "SELECT * FROM vw_operaciones_sucursal" + filtros(p, sucursal, canal, desde, hasta)
                + " ORDER BY fecha_hora DESC, id_transaccion DESC LIMIT 200";
        return jdbc.query(sql, p, (f, n) -> new Operacion(f.getString("numero_transaccion"),
                f.getTimestamp("fecha_hora").toLocalDateTime(), f.getString("tipo_operacion"), f.getString("canal"),
                f.getString("estado_transaccion"), f.getBigDecimal("monto"), f.getString("sucursal"),
                f.getString("empleado"), f.getString("realizado_por")));
    }

    public List<Resumen> resumen(String sucursal, String canal, LocalDateTime desde, LocalDateTime hasta) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String sql = "SELECT sucursal, COALESCE(empleado, 'Banca virtual') AS empleado, tipo_operacion, COUNT(*) AS cantidad,"
                + " SUM(monto) AS total FROM vw_operaciones_sucursal" + filtros(p, sucursal, canal, desde, hasta)
                + " GROUP BY sucursal, COALESCE(empleado, 'Banca virtual'), tipo_operacion ORDER BY sucursal, empleado, tipo_operacion";
        return jdbc.query(sql, p, (f, n) -> new Resumen(f.getString("sucursal"), f.getString("empleado"),
                f.getString("tipo_operacion"), f.getLong("cantidad"), f.getBigDecimal("total")));
    }

    private static String filtros(MapSqlParameterSource p, String sucursal, String canal, LocalDateTime desde, LocalDateTime hasta) {
        StringBuilder sql = new StringBuilder(" WHERE 1 = 1");
        if (sucursal != null && !sucursal.isBlank()) {
            sql.append(" AND sucursal = :sucursal");
            p.addValue("sucursal", sucursal);
        }
        if (canal != null && !canal.isBlank()) {
            sql.append(" AND canal = :canal");
            p.addValue("canal", canal);
        }
        if (desde != null) {
            sql.append(" AND fecha_hora >= :desde");
            p.addValue("desde", Timestamp.valueOf(desde));
        }
        if (hasta != null) {
            sql.append(" AND fecha_hora < :hasta");
            p.addValue("hasta", Timestamp.valueOf(hasta));
        }
        return sql.toString();
    }
}
