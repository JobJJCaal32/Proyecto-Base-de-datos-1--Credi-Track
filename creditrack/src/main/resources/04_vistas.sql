USE credi_track;

CREATE OR REPLACE VIEW vw_cuentas_cliente AS
SELECT c.id_cuenta,
       c.numero_cuenta,
       c.alias,
       c.id_cliente,
       cl.codigo_cliente,
       CONCAT_WS(' ', i.primer_nombre, i.segundo_nombre, i.tercer_nombre, i.primer_apellido, i.segundo_apellido) AS titular,
       tc.nombre AS tipo_cuenta,
       ec.nombre AS estado_cuenta,
       ec.permite_operar,
       c.saldo_actual,
       c.saldo_retenido,
       c.saldo_actual - c.saldo_retenido AS saldo_disponible,
       c.fecha_apertura,
       c.fecha_cierre,
       c.id_sucursal_apertura,
       s.nombre AS sucursal_apertura
  FROM cuenta c
  JOIN cliente cl       ON cl.id_cliente = c.id_cliente
  JOIN identidad i      ON i.id_identidad = cl.id_identidad
  JOIN tipo_cuenta tc   ON tc.id_tipo_cuenta = c.id_tipo_cuenta
  JOIN estado_cuenta ec ON ec.id_estado_cuenta = c.id_estado_cuenta
  JOIN sucursal s       ON s.id_sucursal = c.id_sucursal_apertura;

CREATE OR REPLACE VIEW vw_movimientos AS
SELECT m.id_movimiento,
       t.id_transaccion,
       t.numero_transaccion,
       t.fecha_hora,
       op.nombre AS tipo_operacion,
       et.nombre AS estado_transaccion,
       t.canal,
       t.concepto,
       t.referencia,
       t.id_transaccion_origen,
       m.id_cuenta,
       c.numero_cuenta,
       c.id_cliente,
       cl.codigo_cliente,
       CONCAT_WS(' ', i.primer_nombre, i.primer_apellido, i.segundo_apellido) AS titular,
       m.naturaleza,
       m.monto,
       m.saldo_anterior,
       m.saldo_posterior,
       m.orden,
       cp.numero_cuenta AS cuenta_contraparte,
       CONCAT_WS(' ', ip.primer_nombre, ip.primer_apellido, ip.segundo_apellido) AS titular_contraparte,
       u.username AS realizado_por
  FROM movimiento_cuenta m
  JOIN transaccion t          ON t.id_transaccion = m.id_transaccion
  JOIN tipo_operacion op      ON op.id_tipo_operacion = t.id_tipo_operacion
  JOIN estado_transaccion et  ON et.id_estado_transaccion = t.id_estado_transaccion
  JOIN cuenta c               ON c.id_cuenta = m.id_cuenta
  JOIN cliente cl             ON cl.id_cliente = c.id_cliente
  JOIN identidad i            ON i.id_identidad = cl.id_identidad
  JOIN sesion_usuario su      ON su.id_sesion_usuario = t.id_sesion_usuario
  JOIN usuario u              ON u.id_usuario = su.id_usuario
  LEFT JOIN movimiento_cuenta mp ON mp.id_transaccion = m.id_transaccion AND mp.id_movimiento <> m.id_movimiento
  LEFT JOIN cuenta cp            ON cp.id_cuenta = mp.id_cuenta
  LEFT JOIN cliente clp          ON clp.id_cliente = cp.id_cliente
  LEFT JOIN identidad ip         ON ip.id_identidad = clp.id_identidad;

CREATE OR REPLACE VIEW vw_bitacora_legible AS
SELECT b.id_bitacora,
       b.fecha_hora,
       b.username,
       u.tipo AS tipo_usuario,
       b.id_sesion_usuario,
       su.direccion_ip,
       su.nombre_equipo,
       b.nombre_tabla,
       b.id_registro_afectado,
       b.tipo_accion,
       CASE b.tipo_accion
            WHEN 'INSERT' THEN 'Creación'
            WHEN 'UPDATE' THEN 'Modificación'
            ELSE 'Eliminación'
       END AS accion,
       b.valores_anteriores,
       b.valores_nuevos
  FROM bitacora_auditoria b
  LEFT JOIN sesion_usuario su ON su.id_sesion_usuario = b.id_sesion_usuario
  LEFT JOIN usuario u         ON u.username = b.username;

CREATE OR REPLACE VIEW vw_intentos_fallidos AS
SELECT ia.id_intento,
       ia.fecha_hora,
       ia.username_intentado,
       ia.id_usuario,
       IF(ia.id_usuario IS NULL, 'NO', 'SI') AS usuario_existe,
       u.tipo AS tipo_usuario,
       u.estado AS estado_actual_usuario,
       u.intentos_fallidos AS intentos_acumulados,
       ia.motivo_fallo,
       ia.direccion_ip
  FROM intento_acceso ia
  LEFT JOIN usuario u ON u.id_usuario = ia.id_usuario
 WHERE ia.exitoso = 0;

CREATE OR REPLACE VIEW vw_operaciones_sucursal AS
SELECT t.id_transaccion,
       t.numero_transaccion,
       t.fecha_hora,
       op.nombre AS tipo_operacion,
       t.canal,
       et.nombre AS estado_transaccion,
       t.monto,
       s.id_sucursal,
       COALESCE(s.nombre, 'Banca virtual') AS sucursal,
       emp.id_empleado,
       IF(emp.id_empleado IS NULL, NULL, CONCAT_WS(' ', ie.primer_nombre, ie.primer_apellido)) AS empleado,
       u.username AS realizado_por,
       u.tipo AS tipo_usuario
  FROM transaccion t
  JOIN tipo_operacion op      ON op.id_tipo_operacion = t.id_tipo_operacion
  JOIN estado_transaccion et  ON et.id_estado_transaccion = t.id_estado_transaccion
  JOIN sesion_usuario su      ON su.id_sesion_usuario = t.id_sesion_usuario
  JOIN usuario u              ON u.id_usuario = su.id_usuario
  LEFT JOIN sesion_caja sc    ON sc.id_sesion_caja = t.id_sesion_caja
  LEFT JOIN empleado eu       ON eu.id_empleado = u.id_empleado
  LEFT JOIN empleado emp      ON emp.id_empleado = COALESCE(sc.id_empleado_cajero, eu.id_empleado)
  LEFT JOIN sucursal s        ON s.id_sucursal = COALESCE(sc.id_sucursal, eu.id_sucursal)
  LEFT JOIN identidad ie      ON ie.id_identidad = emp.id_identidad;
