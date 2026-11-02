package gt.umg.creditrack.auditoria;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ConsultaAuditoriaService {

    public static final int TAMANO_PAGINA = 50;

    public record FilaBitacora(Long id, LocalDateTime fechaHora, String username, String tipoUsuario, String direccionIp,
                               String nombreEquipo, String tabla, String registro, String accion, String antes, String despues) {
    }

    public record FilaIntento(Long id, LocalDateTime fechaHora, String username, boolean existe, String tipoUsuario,
                              String estadoActual, String motivo, String direccionIp) {
    }

    public record Pagina<T>(List<T> filas, int numero, boolean haySiguiente) {
    }

    public record FilaConsulta(Long id, LocalDateTime fechaHora, String username, String tipoUsuario, String tipoConsulta,
                               String codigoCliente, String cliente, String numeroCuenta, String criterio, int registros) {
    }

    public record FilaCaja(Integer id, String sucursal, String codigoEmpleado, String cajero, String username,
                           LocalDateTime apertura, LocalDateTime cierre, BigDecimal calculado, BigDecimal declarado,
                           BigDecimal diferencia, String estado, String supervisor, String observaciones) {
    }

    public record TotalesCajas(long cerradas, long cuadradas, long conFaltante, BigDecimal faltante, long conSobrante,
                               BigDecimal sobrante) {

        public BigDecimal neto() {
            return sobrante.subtract(faltante);
        }
    }

    private static final String DESDE_CONSULTAS = """
              FROM bitacora_consulta bc
              LEFT JOIN usuario u   ON u.username = bc.username
              LEFT JOIN cuenta c    ON c.id_cuenta = bc.id_cuenta
              LEFT JOIN cliente cl  ON cl.id_cliente = COALESCE(bc.id_cliente, c.id_cliente)
              LEFT JOIN identidad i ON i.id_identidad = cl.id_identidad
             WHERE 1 = 1
            """;

    private static final String SQL_CAJAS = """
            SELECT sc.id_sesion_caja, s.nombre AS sucursal, e.codigo_empleado,
                   CONCAT_WS(' ', i.primer_nombre, i.primer_apellido) AS cajero, u.username,
                   sc.fecha_apertura, sc.fecha_cierre, sc.monto_cierre_calculado, sc.monto_cierre_declarado,
                   sc.diferencia, sc.estado, CONCAT_WS(' ', isup.primer_nombre, isup.primer_apellido) AS supervisor,
                   sc.observaciones
              FROM sesion_caja sc
              JOIN sucursal s        ON s.id_sucursal = sc.id_sucursal
              JOIN empleado e        ON e.id_empleado = sc.id_empleado_cajero
              JOIN identidad i       ON i.id_identidad = e.id_identidad
              LEFT JOIN usuario u    ON u.id_empleado = e.id_empleado
              LEFT JOIN empleado es  ON es.id_empleado = sc.id_empleado_supervisor
              LEFT JOIN identidad isup ON isup.id_identidad = es.id_identidad
             WHERE sc.estado <> 'ABIERTA'
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ConsultaAuditoriaService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<String> tablasAuditadas() {
        return jdbc.getJdbcTemplate().queryForList(
                "SELECT DISTINCT nombre_tabla FROM vw_bitacora_legible ORDER BY nombre_tabla", String.class);
    }

    public Pagina<FilaBitacora> bitacora(String tabla, String usuario, String accion, LocalDateTime desde,
                                         LocalDateTime hasta, int pagina) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("SELECT * FROM vw_bitacora_legible WHERE 1 = 1");
        filtro(sql, p, "tabla", tabla, " AND nombre_tabla = :tabla");
        filtro(sql, p, "usuario", usuario, " AND username = :usuario");
        filtro(sql, p, "accion", accion, " AND tipo_accion = :accion");
        rango(sql, p, desde, hasta);
        int numero = Math.max(pagina, 1);
        sql.append(" ORDER BY id_bitacora DESC LIMIT :limite OFFSET :desplazamiento");
        p.addValue("limite", TAMANO_PAGINA + 1).addValue("desplazamiento", (numero - 1) * TAMANO_PAGINA);
        List<FilaBitacora> filas = jdbc.query(sql.toString(), p, (f, n) -> new FilaBitacora(
                f.getLong("id_bitacora"), f.getTimestamp("fecha_hora").toLocalDateTime(), f.getString("username"),
                f.getString("tipo_usuario"), f.getString("direccion_ip"), f.getString("nombre_equipo"),
                f.getString("nombre_tabla"), f.getString("id_registro_afectado"), f.getString("accion"),
                f.getString("valores_anteriores"), f.getString("valores_nuevos")));
        return recortar(filas, numero);
    }

    public Pagina<FilaIntento> intentos(String usuario, String ip, LocalDateTime desde, LocalDateTime hasta, int pagina) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("SELECT * FROM vw_intentos_fallidos WHERE 1 = 1");
        filtro(sql, p, "usuario", usuario, " AND username_intentado = :usuario");
        filtro(sql, p, "ip", ip, " AND direccion_ip = :ip");
        rango(sql, p, desde, hasta);
        int numero = Math.max(pagina, 1);
        sql.append(" ORDER BY id_intento DESC LIMIT :limite OFFSET :desplazamiento");
        p.addValue("limite", TAMANO_PAGINA + 1).addValue("desplazamiento", (numero - 1) * TAMANO_PAGINA);
        List<FilaIntento> filas = jdbc.query(sql.toString(), p, (f, n) -> new FilaIntento(
                f.getLong("id_intento"), f.getTimestamp("fecha_hora").toLocalDateTime(), f.getString("username_intentado"),
                "SI".equals(f.getString("usuario_existe")), f.getString("tipo_usuario"), f.getString("estado_actual_usuario"),
                f.getString("motivo_fallo"), f.getString("direccion_ip")));
        return recortar(filas, numero);
    }

    public List<Map<String, Object>> resumenIntentos(String agrupador, LocalDateTime desde, LocalDateTime hasta) {
        String columna = "ip".equals(agrupador) ? "direccion_ip" : "username_intentado";
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("SELECT " + columna + " AS clave, COUNT(*) AS cantidad, MAX(fecha_hora) AS ultimo"
                + " FROM vw_intentos_fallidos WHERE 1 = 1");
        rango(sql, p, desde, hasta);
        sql.append(" GROUP BY ").append(columna).append(" ORDER BY cantidad DESC, ultimo DESC LIMIT 10");
        return jdbc.queryForList(sql.toString(), p);
    }

    public Pagina<FilaConsulta> consultas(String usuario, String cliente, String tipo, String quien, LocalDateTime desde,
                                          LocalDateTime hasta, int pagina) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("""
                SELECT bc.id_bitacora_consulta, bc.fecha_hora, bc.username, u.tipo AS tipo_usuario, bc.tipo_consulta,
                       cl.codigo_cliente, CONCAT_WS(' ', i.primer_nombre, i.primer_apellido, i.segundo_apellido) AS cliente,
                       c.numero_cuenta, bc.criterio, bc.cantidad_registros
                """).append(DESDE_CONSULTAS);
        filtrosConsultas(sql, p, usuario, cliente, tipo, quien, desde, hasta);
        int numero = Math.max(pagina, 1);
        sql.append(" ORDER BY bc.id_bitacora_consulta DESC LIMIT :limite OFFSET :desplazamiento");
        p.addValue("limite", TAMANO_PAGINA + 1).addValue("desplazamiento", (numero - 1) * TAMANO_PAGINA);
        List<FilaConsulta> filas = jdbc.query(sql.toString(), p, (f, n) -> new FilaConsulta(
                f.getLong("id_bitacora_consulta"), f.getTimestamp("fecha_hora").toLocalDateTime(), f.getString("username"),
                f.getString("tipo_usuario"), f.getString("tipo_consulta"), f.getString("codigo_cliente"),
                f.getString("cliente"), f.getString("numero_cuenta"), f.getString("criterio"), f.getInt("cantidad_registros")));
        return recortar(filas, numero);
    }

    public List<Map<String, Object>> resumenConsultas(String agrupador, String usuario, String cliente, String tipo, String quien,
                                                      LocalDateTime desde, LocalDateTime hasta) {
        boolean porCliente = "cliente".equals(agrupador);
        String clave = porCliente ? "cl.codigo_cliente" : "bc.username";
        String nombre = porCliente ? "CONCAT_WS(' ', i.primer_nombre, i.primer_apellido)" : "u.tipo";
        String otro = porCliente ? "bc.username" : "cl.codigo_cliente";
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("SELECT " + clave + " AS clave, MAX(" + nombre + ") AS nombre, COUNT(*) AS cantidad,"
                + " COUNT(DISTINCT " + otro + ") AS distintos").append(DESDE_CONSULTAS);
        filtrosConsultas(sql, p, usuario, cliente, tipo, quien, desde, hasta);
        sql.append(" AND ").append(clave).append(" IS NOT NULL GROUP BY ").append(clave).append(" ORDER BY cantidad DESC, clave LIMIT 10");
        return jdbc.queryForList(sql.toString(), p);
    }

    public List<Map<String, Object>> sucursales() {
        return jdbc.getJdbcTemplate().queryForList("SELECT id_sucursal AS id, nombre FROM sucursal ORDER BY nombre");
    }

    public Pagina<FilaCaja> cajas(Integer idSucursal, String cajero, String resultado, LocalDateTime desde, LocalDateTime hasta,
                                  int pagina) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder(SQL_CAJAS);
        filtrosCajas(sql, p, idSucursal, cajero, desde, hasta);
        switch (resultado == null ? "" : resultado) {
            case "FALTANTE" -> sql.append(" AND sc.diferencia < 0");
            case "SOBRANTE" -> sql.append(" AND sc.diferencia > 0");
            case "CUADRADA" -> sql.append(" AND sc.diferencia = 0");
            case "DIFERENCIA" -> sql.append(" AND sc.diferencia <> 0");
            default -> {
            }
        }
        int numero = Math.max(pagina, 1);
        sql.append(" ORDER BY sc.fecha_cierre DESC, sc.id_sesion_caja DESC LIMIT :limite OFFSET :desplazamiento");
        p.addValue("limite", TAMANO_PAGINA + 1).addValue("desplazamiento", (numero - 1) * TAMANO_PAGINA);
        List<FilaCaja> filas = jdbc.query(sql.toString(), p, (f, n) -> new FilaCaja(
                f.getInt("id_sesion_caja"), f.getString("sucursal"), f.getString("codigo_empleado"), f.getString("cajero"),
                f.getString("username"), f.getTimestamp("fecha_apertura").toLocalDateTime(),
                f.getTimestamp("fecha_cierre").toLocalDateTime(), f.getBigDecimal("monto_cierre_calculado"),
                f.getBigDecimal("monto_cierre_declarado"), f.getBigDecimal("diferencia"), f.getString("estado"),
                vacioANulo(f.getString("supervisor")), f.getString("observaciones")));
        return recortar(filas, numero);
    }

    public TotalesCajas totalesCajas(Integer idSucursal, String cajero, LocalDateTime desde, LocalDateTime hasta) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS cerradas, COALESCE(SUM(x.diferencia = 0), 0) AS cuadradas,"
                + " COALESCE(SUM(x.diferencia < 0), 0) AS con_faltante, COALESCE(-SUM(LEAST(x.diferencia, 0)), 0) AS faltante,"
                + " COALESCE(SUM(x.diferencia > 0), 0) AS con_sobrante, COALESCE(SUM(GREATEST(x.diferencia, 0)), 0) AS sobrante FROM ("
                + SQL_CAJAS);
        filtrosCajas(sql, p, idSucursal, cajero, desde, hasta);
        sql.append(") x");
        return jdbc.queryForObject(sql.toString(), p, (f, n) -> new TotalesCajas(f.getLong("cerradas"), f.getLong("cuadradas"),
                f.getLong("con_faltante"), f.getBigDecimal("faltante"), f.getLong("con_sobrante"), f.getBigDecimal("sobrante")));
    }

    private static void filtrosConsultas(StringBuilder sql, MapSqlParameterSource p, String usuario, String cliente, String tipo,
                                         String quien, LocalDateTime desde, LocalDateTime hasta) {
        filtro(sql, p, "usuario", usuario, " AND bc.username = :usuario");
        filtro(sql, p, "cliente", cliente == null ? null : cliente.trim().toUpperCase(Locale.ROOT),
                " AND cl.codigo_cliente = :cliente");
        filtro(sql, p, "tipo", tipo, " AND bc.tipo_consulta = :tipo");
        filtro(sql, p, "quien", quien, " AND u.tipo = :quien");
        rango(sql, p, "bc.fecha_hora", desde, hasta);
    }

    private static void filtrosCajas(StringBuilder sql, MapSqlParameterSource p, Integer idSucursal, String cajero,
                                     LocalDateTime desde, LocalDateTime hasta) {
        if (idSucursal != null) {
            sql.append(" AND sc.id_sucursal = :sucursal");
            p.addValue("sucursal", idSucursal);
        }
        if (cajero != null && !cajero.isBlank()) {
            sql.append(" AND (e.codigo_empleado = :cajero OR u.username = :cajero)");
            p.addValue("cajero", cajero.trim());
        }
        rango(sql, p, "sc.fecha_cierre", desde, hasta);
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto;
    }

    private static void filtro(StringBuilder sql, MapSqlParameterSource p, String nombre, String valor, String condicion) {
        if (valor != null && !valor.isBlank()) {
            sql.append(condicion);
            p.addValue(nombre, valor.trim());
        }
    }

    private static void rango(StringBuilder sql, MapSqlParameterSource p, LocalDateTime desde, LocalDateTime hasta) {
        rango(sql, p, "fecha_hora", desde, hasta);
    }

    private static void rango(StringBuilder sql, MapSqlParameterSource p, String columna, LocalDateTime desde, LocalDateTime hasta) {
        if (desde != null) {
            sql.append(" AND ").append(columna).append(" >= :desde");
            p.addValue("desde", Timestamp.valueOf(desde));
        }
        if (hasta != null) {
            sql.append(" AND ").append(columna).append(" < :hasta");
            p.addValue("hasta", Timestamp.valueOf(hasta));
        }
    }

    private static <T> Pagina<T> recortar(List<T> filas, int numero) {
        boolean siguiente = filas.size() > TAMANO_PAGINA;
        return new Pagina<>(siguiente ? filas.subList(0, TAMANO_PAGINA) : filas, numero, siguiente);
    }
}
