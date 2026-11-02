package gt.umg.creditrack.cuenta;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ConsultaCuentasService {

    private final JdbcTemplate jdbc;

    public ConsultaCuentasService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CuentaClienteFila> deCliente(Long idSesionUsuario, Integer idCliente) {
        return jdbc.query("CALL sp_consultar_cuentas(?, ?)", (fila, n) -> new CuentaClienteFila(
                fila.getInt("id_cuenta"),
                fila.getString("numero_cuenta"),
                fila.getString("alias"),
                fila.getString("tipo_cuenta"),
                fila.getString("estado_cuenta"),
                fila.getBoolean("permite_operar"),
                fila.getBigDecimal("saldo_actual"),
                fila.getBigDecimal("saldo_retenido"),
                fila.getBigDecimal("saldo_disponible"),
                fecha(fila.getDate("fecha_apertura")),
                fecha(fila.getDate("fecha_cierre")),
                fila.getString("sucursal_apertura")), idSesionUsuario, idCliente);
    }

    private static LocalDate fecha(Date fecha) {
        return fecha == null ? null : fecha.toLocalDate();
    }
}
