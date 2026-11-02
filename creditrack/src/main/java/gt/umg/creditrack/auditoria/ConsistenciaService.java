package gt.umg.creditrack.auditoria;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsistenciaService {

    public static final int MAXIMO_CASOS = 20;

    public record Comprobacion(String titulo, String descripcion, String unidad, long revisados, long fallas, List<String> casos) {

        public boolean cuadra() {
            return fallas == 0;
        }
    }

    public record Resultado(LocalDateTime fechaHora, List<Comprobacion> comprobaciones) {

        public long conFallas() {
            return comprobaciones.stream().filter(c -> !c.cuadra()).count();
        }

        public boolean todoCuadra() {
            return conFallas() == 0;
        }
    }

    private record Regla(String titulo, String descripcion, String unidad, String sqlRevisados, String sqlFallas) {
    }

    private static final List<Regla> REGLAS = List.of(
            new Regla("Saldos de las cuentas",
                    "El saldo de cada cuenta es igual a la suma de sus abonos menos sus cargos.",
                    "cuentas",
                    "SELECT COUNT(*) FROM cuenta",
                    """
                    SELECT CONCAT('Cuenta ', c.numero_cuenta, ': saldo Q ', FORMAT(c.saldo_actual, 2),
                                  ', sus movimientos suman Q ', FORMAT(COALESCE(m.total, 0), 2)) AS detalle
                      FROM cuenta c
                      LEFT JOIN (SELECT id_cuenta, SUM(IF(naturaleza = 'ABONO', monto, -monto)) AS total
                                   FROM movimiento_cuenta GROUP BY id_cuenta) m ON m.id_cuenta = c.id_cuenta
                     WHERE c.saldo_actual <> COALESCE(m.total, 0)
                     ORDER BY c.numero_cuenta
                    """),
            new Regla("Cadena de movimientos",
                    "Cada movimiento empieza con el saldo en que terminó el movimiento anterior de la misma cuenta; el primero empieza en cero.",
                    "movimientos",
                    "SELECT COUNT(*) FROM movimiento_cuenta",
                    """
                    SELECT CONCAT('Cuenta ', c.numero_cuenta, ', ', t.numero_transaccion, ': empieza en Q ',
                                  FORMAT(x.saldo_anterior, 2), ' y el movimiento anterior terminó en Q ', FORMAT(x.previo, 2)) AS detalle
                      FROM (SELECT m.id_movimiento, m.id_cuenta, m.id_transaccion, m.saldo_anterior,
                                   COALESCE(LAG(m.saldo_posterior) OVER (PARTITION BY m.id_cuenta ORDER BY m.id_movimiento), 0) AS previo
                              FROM movimiento_cuenta m) x
                      JOIN cuenta c      ON c.id_cuenta = x.id_cuenta
                      JOIN transaccion t ON t.id_transaccion = x.id_transaccion
                     WHERE x.saldo_anterior <> x.previo
                     ORDER BY x.id_movimiento
                    """),
            new Regla("Movimientos de cada transacción",
                    "Un depósito aplicado tiene un abono, un retiro un cargo y una transferencia un cargo y un abono, todos por el monto de la transacción. Una pendiente o rechazada no tiene movimientos.",
                    "transacciones",
                    "SELECT COUNT(*) FROM transaccion",
                    """
                    SELECT CONCAT(x.numero_transaccion, ': ', LOWER(x.operacion), ' ', LOWER(x.estado), ' con ', x.movimientos,
                                  ' movimiento(s), ', x.cargos, ' cargo(s), ', x.abonos, ' abono(s) y ', x.distintos,
                                  ' por un monto distinto') AS detalle
                      FROM (SELECT t.id_transaccion, t.numero_transaccion, op.nombre AS operacion, et.nombre AS estado,
                                   COUNT(m.id_movimiento) AS movimientos,
                                   COALESCE(SUM(m.naturaleza = 'CARGO'), 0) AS cargos,
                                   COALESCE(SUM(m.naturaleza = 'ABONO'), 0) AS abonos,
                                   COALESCE(SUM(m.monto <> t.monto), 0) AS distintos
                              FROM transaccion t
                              JOIN tipo_operacion op     ON op.id_tipo_operacion = t.id_tipo_operacion
                              JOIN estado_transaccion et ON et.id_estado_transaccion = t.id_estado_transaccion
                              LEFT JOIN movimiento_cuenta m ON m.id_transaccion = t.id_transaccion
                             GROUP BY t.id_transaccion, t.numero_transaccion, op.nombre, et.nombre) x
                     WHERE NOT (
                           (x.estado IN ('PENDIENTE', 'RECHAZADA') AND x.movimientos = 0)
                        OR (x.estado IN ('APLICADA', 'REVERSADA') AND x.distintos = 0 AND (
                               (x.operacion = 'DEPOSITO' AND x.movimientos = 1 AND x.abonos = 1)
                            OR (x.operacion = 'RETIRO' AND x.movimientos = 1 AND x.cargos = 1)
                            OR (x.operacion = 'TRANSFERENCIA' AND x.movimientos = 2 AND x.cargos = 1 AND x.abonos = 1))))
                     ORDER BY x.id_transaccion
                    """),
            new Regla("Reversos",
                    "Cada reverso anula una transferencia marcada como REVERSADA, por el mismo monto y con las cuentas al revés. Cada transferencia reversada tiene un solo reverso.",
                    "reversos",
                    "SELECT COUNT(*) FROM transaccion WHERE id_transaccion_origen IS NOT NULL",
                    """
                    SELECT CONCAT(r.numero_transaccion, ' no invierte correctamente a ', o.numero_transaccion) AS detalle
                      FROM transaccion r
                      JOIN transaccion o          ON o.id_transaccion = r.id_transaccion_origen
                      JOIN estado_transaccion eo  ON eo.id_estado_transaccion = o.id_estado_transaccion
                      LEFT JOIN movimiento_cuenta rc ON rc.id_transaccion = r.id_transaccion AND rc.naturaleza = 'CARGO'
                      LEFT JOIN movimiento_cuenta ra ON ra.id_transaccion = r.id_transaccion AND ra.naturaleza = 'ABONO'
                      LEFT JOIN movimiento_cuenta oc ON oc.id_transaccion = o.id_transaccion AND oc.naturaleza = 'CARGO'
                      LEFT JOIN movimiento_cuenta oa ON oa.id_transaccion = o.id_transaccion AND oa.naturaleza = 'ABONO'
                     WHERE eo.nombre <> 'REVERSADA' OR r.monto <> o.monto
                        OR NOT (rc.id_cuenta <=> oa.id_cuenta) OR NOT (ra.id_cuenta <=> oc.id_cuenta)
                    UNION ALL
                    SELECT CONCAT(o.numero_transaccion, ' está REVERSADA y no tiene reverso')
                      FROM transaccion o
                      JOIN estado_transaccion eo ON eo.id_estado_transaccion = o.id_estado_transaccion
                     WHERE eo.nombre = 'REVERSADA'
                       AND NOT EXISTS (SELECT 1 FROM transaccion r WHERE r.id_transaccion_origen = o.id_transaccion)
                    UNION ALL
                    SELECT CONCAT(o.numero_transaccion, ' tiene ', COUNT(*), ' reversos')
                      FROM transaccion r
                      JOIN transaccion o ON o.id_transaccion = r.id_transaccion_origen
                     GROUP BY o.id_transaccion, o.numero_transaccion
                    HAVING COUNT(*) > 1
                    """),
            new Regla("Cuentas cerradas",
                    "Una cuenta cerrada no tiene saldo ni saldo retenido y tiene fecha de cierre; una cuenta que no está cerrada no tiene fecha de cierre.",
                    "cuentas",
                    "SELECT COUNT(*) FROM cuenta",
                    """
                    SELECT CONCAT('Cuenta ', c.numero_cuenta, ' (', ec.nombre, '): saldo Q ', FORMAT(c.saldo_actual, 2),
                                  ', retenido Q ', FORMAT(c.saldo_retenido, 2), ', fecha de cierre ',
                                  COALESCE(DATE_FORMAT(c.fecha_cierre, '%d/%m/%Y'), 'sin fecha')) AS detalle
                      FROM cuenta c
                      JOIN estado_cuenta ec ON ec.id_estado_cuenta = c.id_estado_cuenta
                     WHERE (ec.nombre = 'CERRADA' AND (c.saldo_actual <> 0 OR c.saldo_retenido <> 0 OR c.fecha_cierre IS NULL))
                        OR (ec.nombre <> 'CERRADA' AND c.fecha_cierre IS NOT NULL)
                     ORDER BY c.numero_cuenta
                    """),
            new Regla("Cierre de cajas",
                    "En cada caja cerrada, el efectivo esperado es el inicial más los depósitos menos los retiros aplicados, y la diferencia es lo contado menos lo esperado.",
                    "cajas cerradas",
                    "SELECT COUNT(*) FROM sesion_caja WHERE estado <> 'ABIERTA'",
                    """
                    SELECT CONCAT('Caja ', sc.id_sesion_caja, ': esperado guardado Q ', FORMAT(sc.monto_cierre_calculado, 2),
                                  ', recalculado Q ', FORMAT(sc.monto_apertura + COALESCE(x.neto, 0), 2),
                                  '; diferencia guardada Q ', FORMAT(sc.diferencia, 2)) AS detalle
                      FROM sesion_caja sc
                      LEFT JOIN (SELECT t.id_sesion_caja,
                                        SUM(CASE op.nombre WHEN 'DEPOSITO' THEN t.monto WHEN 'RETIRO' THEN -t.monto ELSE 0 END) AS neto
                                   FROM transaccion t
                                   JOIN tipo_operacion op     ON op.id_tipo_operacion = t.id_tipo_operacion
                                   JOIN estado_transaccion et ON et.id_estado_transaccion = t.id_estado_transaccion
                                  WHERE t.id_sesion_caja IS NOT NULL AND et.nombre IN ('APLICADA', 'REVERSADA')
                                  GROUP BY t.id_sesion_caja) x ON x.id_sesion_caja = sc.id_sesion_caja
                     WHERE sc.estado <> 'ABIERTA'
                       AND (NOT (sc.monto_cierre_calculado <=> sc.monto_apertura + COALESCE(x.neto, 0))
                            OR NOT (sc.diferencia <=> sc.monto_cierre_declarado - sc.monto_cierre_calculado))
                     ORDER BY sc.id_sesion_caja
                    """),
            new Regla("Rastro en la bitácora",
                    "Cada transacción y cada cuenta tiene su registro de creación en la bitácora de auditoría.",
                    "registros",
                    "SELECT (SELECT COUNT(*) FROM transaccion) + (SELECT COUNT(*) FROM cuenta)",
                    """
                    SELECT CONCAT('Transacción ', t.numero_transaccion, ' sin registro en la bitácora') AS detalle
                      FROM transaccion t
                     WHERE NOT EXISTS (SELECT 1 FROM bitacora_auditoria b
                                        WHERE b.nombre_tabla = 'transaccion' AND b.tipo_accion = 'INSERT'
                                          AND b.id_registro_afectado = CAST(t.id_transaccion AS CHAR) COLLATE utf8mb4_spanish_ci)
                    UNION ALL
                    SELECT CONCAT('Cuenta ', c.numero_cuenta, ' sin registro en la bitácora')
                      FROM cuenta c
                     WHERE NOT EXISTS (SELECT 1 FROM bitacora_auditoria b
                                        WHERE b.nombre_tabla = 'cuenta' AND b.tipo_accion = 'INSERT'
                                          AND b.id_registro_afectado = CAST(c.id_cuenta AS CHAR) COLLATE utf8mb4_spanish_ci)
                    """));

    private final JdbcTemplate jdbc;

    public ConsistenciaService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public Resultado verificar() {
        List<Comprobacion> comprobaciones = REGLAS.stream().map(this::comprobar).toList();
        return new Resultado(LocalDateTime.now(ZoneOffset.UTC), comprobaciones);
    }

    private Comprobacion comprobar(Regla regla) {
        Long revisados = jdbc.queryForObject(regla.sqlRevisados(), Long.class);
        Long fallas = jdbc.queryForObject("SELECT COUNT(*) FROM (" + regla.sqlFallas() + ") f", Long.class);
        List<String> casos = fallas == null || fallas == 0 ? List.of()
                : jdbc.queryForList("SELECT detalle FROM (" + regla.sqlFallas() + ") f LIMIT " + MAXIMO_CASOS, String.class);
        return new Comprobacion(regla.titulo(), regla.descripcion(), regla.unidad(),
                revisados == null ? 0 : revisados, fallas == null ? 0 : fallas, casos);
    }
}
