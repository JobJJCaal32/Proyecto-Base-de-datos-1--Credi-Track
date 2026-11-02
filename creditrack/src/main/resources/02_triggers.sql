USE credi_track;

DELIMITER $$

DROP TRIGGER IF EXISTS trg_identidad_bi $$
CREATE TRIGGER trg_identidad_bi BEFORE INSERT ON identidad FOR EACH ROW
BEGIN
    IF NEW.fecha_nacimiento > DATE_SUB(UTC_DATE(), INTERVAL 18 YEAR) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La persona debe ser mayor de 18 años';
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_identidad_bu $$
CREATE TRIGGER trg_identidad_bu BEFORE UPDATE ON identidad FOR EACH ROW
BEGIN
    IF NEW.fecha_nacimiento <> OLD.fecha_nacimiento
       AND NEW.fecha_nacimiento > DATE_SUB(UTC_DATE(), INTERVAL 18 YEAR) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La persona debe ser mayor de 18 años';
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_cuenta_bi $$
CREATE TRIGGER trg_cuenta_bi BEFORE INSERT ON cuenta FOR EACH ROW
BEGIN
    IF NEW.saldo_actual <> 0 OR NEW.saldo_retenido <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Una cuenta se abre con saldo 0: el dinero entra con una transacción';
    END IF;
    IF NEW.fecha_cierre IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Una cuenta nueva no puede tener fecha de cierre';
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_cuenta_bu $$
CREATE TRIGGER trg_cuenta_bu BEFORE UPDATE ON cuenta FOR EACH ROW
BEGIN
    DECLARE v_estado_anterior VARCHAR(20);
    DECLARE v_estado_nuevo VARCHAR(20);

    IF (NEW.saldo_actual <> OLD.saldo_actual OR NEW.saldo_retenido <> OLD.saldo_retenido)
       AND COALESCE(@permitir_cambio_saldo, 0) <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El saldo solo se modifica dentro de una transacción de dinero';
    END IF;

    SELECT nombre INTO v_estado_anterior FROM estado_cuenta WHERE id_estado_cuenta = OLD.id_estado_cuenta;
    SELECT nombre INTO v_estado_nuevo FROM estado_cuenta WHERE id_estado_cuenta = NEW.id_estado_cuenta;

    IF v_estado_anterior = 'CERRADA' AND v_estado_nuevo <> 'CERRADA' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Una cuenta cerrada no se puede reabrir';
    END IF;

    IF v_estado_anterior = 'CERRADA' AND NEW.saldo_actual <> OLD.saldo_actual THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Una cuenta cerrada no puede recibir ni enviar dinero';
    END IF;

    IF v_estado_nuevo = 'CERRADA' AND v_estado_anterior <> 'CERRADA' THEN
        IF NEW.saldo_actual <> 0 OR NEW.saldo_retenido <> 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo se puede cerrar una cuenta con saldo 0';
        END IF;
        IF NEW.fecha_cierre IS NULL THEN
            SET NEW.fecha_cierre = UTC_DATE();
        END IF;
    END IF;

    IF v_estado_nuevo <> 'CERRADA' AND NEW.fecha_cierre IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo una cuenta cerrada puede tener fecha de cierre';
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_transaccion_bi $$
CREATE TRIGGER trg_transaccion_bi BEFORE INSERT ON transaccion FOR EACH ROW
BEGIN
    IF NEW.id_transaccion_origen IS NOT NULL AND NEW.id_transaccion_origen = NEW.id_transaccion THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Una transacción no puede ser reverso de sí misma';
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_transaccion_bu $$
CREATE TRIGGER trg_transaccion_bu BEFORE UPDATE ON transaccion FOR EACH ROW
BEGIN
    IF NEW.numero_transaccion <> OLD.numero_transaccion
       OR NEW.id_tipo_operacion <> OLD.id_tipo_operacion
       OR NEW.monto <> OLD.monto
       OR NEW.fecha_hora <> OLD.fecha_hora
       OR NEW.canal <> OLD.canal
       OR NEW.id_sesion_usuario <> OLD.id_sesion_usuario
       OR NOT (NEW.id_sesion_caja <=> OLD.id_sesion_caja)
       OR NOT (NEW.concepto <=> OLD.concepto)
       OR NEW.referencia <> OLD.referencia
       OR NOT (NEW.id_transaccion_origen <=> OLD.id_transaccion_origen)
       OR NEW.identidad_verificada <> OLD.identidad_verificada THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'De una transacción registrada solo se puede cambiar su estado';
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_movimiento_cuenta_bu $$
CREATE TRIGGER trg_movimiento_cuenta_bu BEFORE UPDATE ON movimiento_cuenta FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Los movimientos de cuenta no se modifican' $$

DROP TRIGGER IF EXISTS trg_intento_acceso_bu $$
CREATE TRIGGER trg_intento_acceso_bu BEFORE UPDATE ON intento_acceso FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El registro de intentos de acceso no se modifica' $$

DROP TRIGGER IF EXISTS trg_bitacora_auditoria_bu $$
CREATE TRIGGER trg_bitacora_auditoria_bu BEFORE UPDATE ON bitacora_auditoria FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La bitácora de auditoría no se modifica' $$

DROP TRIGGER IF EXISTS trg_bitacora_consulta_bu $$
CREATE TRIGGER trg_bitacora_consulta_bu BEFORE UPDATE ON bitacora_consulta FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La bitácora de consultas no se modifica' $$

DROP TRIGGER IF EXISTS trg_identidad_bd $$
CREATE TRIGGER trg_identidad_bd BEFORE DELETE ON identidad FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se eliminan identidades: usar baja lógica' $$

DROP TRIGGER IF EXISTS trg_direccion_bd $$
CREATE TRIGGER trg_direccion_bd BEFORE DELETE ON direccion FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se eliminan direcciones' $$

DROP TRIGGER IF EXISTS trg_sucursal_bd $$
CREATE TRIGGER trg_sucursal_bd BEFORE DELETE ON sucursal FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se eliminan sucursales: cambiar su estado a INACTIVA' $$

DROP TRIGGER IF EXISTS trg_cliente_bd $$
CREATE TRIGGER trg_cliente_bd BEFORE DELETE ON cliente FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se eliminan clientes: usar estado INACTIVO y fecha de baja' $$

DROP TRIGGER IF EXISTS trg_empleado_bd $$
CREATE TRIGGER trg_empleado_bd BEFORE DELETE ON empleado FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se eliminan empleados: usar estado INACTIVO y fecha de baja' $$

DROP TRIGGER IF EXISTS trg_usuario_bd $$
CREATE TRIGGER trg_usuario_bd BEFORE DELETE ON usuario FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se eliminan usuarios: usar estado INACTIVO' $$

DROP TRIGGER IF EXISTS trg_cuenta_bd $$
CREATE TRIGGER trg_cuenta_bd BEFORE DELETE ON cuenta FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se eliminan cuentas: cambiar su estado a CERRADA' $$

DROP TRIGGER IF EXISTS trg_transaccion_bd $$
CREATE TRIGGER trg_transaccion_bd BEFORE DELETE ON transaccion FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se eliminan transacciones: se corrigen con un reverso' $$

DROP TRIGGER IF EXISTS trg_movimiento_cuenta_bd $$
CREATE TRIGGER trg_movimiento_cuenta_bd BEFORE DELETE ON movimiento_cuenta FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Los movimientos de cuenta no se eliminan' $$

DROP TRIGGER IF EXISTS trg_autorizacion_bd $$
CREATE TRIGGER trg_autorizacion_bd BEFORE DELETE ON autorizacion FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se eliminan autorizaciones' $$

DROP TRIGGER IF EXISTS trg_sesion_caja_bd $$
CREATE TRIGGER trg_sesion_caja_bd BEFORE DELETE ON sesion_caja FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se eliminan sesiones de caja' $$

DROP TRIGGER IF EXISTS trg_sesion_usuario_bd $$
CREATE TRIGGER trg_sesion_usuario_bd BEFORE DELETE ON sesion_usuario FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se eliminan sesiones de usuario' $$

DROP TRIGGER IF EXISTS trg_intento_acceso_bd $$
CREATE TRIGGER trg_intento_acceso_bd BEFORE DELETE ON intento_acceso FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El registro de intentos de acceso no se elimina' $$

DROP TRIGGER IF EXISTS trg_alerta_bd $$
CREATE TRIGGER trg_alerta_bd BEFORE DELETE ON alerta FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se eliminan alertas: se resuelven o descartan' $$

DROP TRIGGER IF EXISTS trg_estado_cuenta_generado_bd $$
CREATE TRIGGER trg_estado_cuenta_generado_bd BEFORE DELETE ON estado_cuenta_generado FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se eliminan estados de cuenta generados' $$

DROP TRIGGER IF EXISTS trg_bitacora_auditoria_bd $$
CREATE TRIGGER trg_bitacora_auditoria_bd BEFORE DELETE ON bitacora_auditoria FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La bitácora de auditoría no se elimina' $$

DROP TRIGGER IF EXISTS trg_bitacora_consulta_bd $$
CREATE TRIGGER trg_bitacora_consulta_bd BEFORE DELETE ON bitacora_consulta FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La bitácora de consultas no se elimina' $$

DROP TRIGGER IF EXISTS trg_cliente_ai $$
CREATE TRIGGER trg_cliente_ai AFTER INSERT ON cliente FOR EACH ROW
    INSERT INTO bitacora_auditoria (id_sesion_usuario, username, nombre_tabla, id_registro_afectado, tipo_accion, valores_anteriores, valores_nuevos, fecha_hora)
    VALUES (@id_sesion_usuario, LEFT(COALESCE(CONVERT(@usuario_app USING utf8mb4), CONVERT(SUBSTRING_INDEX(USER(), '@', 1) USING utf8mb4)), 30), 'cliente', NEW.id_cliente, 'INSERT', NULL,
            JSON_OBJECT('codigo_cliente', NEW.codigo_cliente, 'id_identidad', NEW.id_identidad, 'estado', NEW.estado,
                        'fecha_alta', NEW.fecha_alta, 'id_usuario_registro', NEW.id_usuario_registro),
            UTC_TIMESTAMP()) $$

DROP TRIGGER IF EXISTS trg_cliente_au $$
CREATE TRIGGER trg_cliente_au AFTER UPDATE ON cliente FOR EACH ROW
BEGIN
    DECLARE v_antes JSON;
    DECLARE v_despues JSON;
    SET v_antes = JSON_OBJECT('estado', OLD.estado, 'fecha_baja', OLD.fecha_baja);
    SET v_despues = JSON_OBJECT('estado', NEW.estado, 'fecha_baja', NEW.fecha_baja);
    IF v_antes <> v_despues THEN
        INSERT INTO bitacora_auditoria (id_sesion_usuario, username, nombre_tabla, id_registro_afectado, tipo_accion, valores_anteriores, valores_nuevos, fecha_hora)
        VALUES (@id_sesion_usuario, LEFT(COALESCE(CONVERT(@usuario_app USING utf8mb4), CONVERT(SUBSTRING_INDEX(USER(), '@', 1) USING utf8mb4)), 30), 'cliente', NEW.id_cliente, 'UPDATE', v_antes, v_despues, UTC_TIMESTAMP());
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_empleado_ai $$
CREATE TRIGGER trg_empleado_ai AFTER INSERT ON empleado FOR EACH ROW
    INSERT INTO bitacora_auditoria (id_sesion_usuario, username, nombre_tabla, id_registro_afectado, tipo_accion, valores_anteriores, valores_nuevos, fecha_hora)
    VALUES (@id_sesion_usuario, LEFT(COALESCE(CONVERT(@usuario_app USING utf8mb4), CONVERT(SUBSTRING_INDEX(USER(), '@', 1) USING utf8mb4)), 30), 'empleado', NEW.id_empleado, 'INSERT', NULL,
            JSON_OBJECT('codigo_empleado', NEW.codigo_empleado, 'id_identidad', NEW.id_identidad, 'id_sucursal', NEW.id_sucursal,
                        'puesto', NEW.puesto, 'estado', NEW.estado, 'id_usuario_registro', NEW.id_usuario_registro),
            UTC_TIMESTAMP()) $$

DROP TRIGGER IF EXISTS trg_empleado_au $$
CREATE TRIGGER trg_empleado_au AFTER UPDATE ON empleado FOR EACH ROW
BEGIN
    DECLARE v_antes JSON;
    DECLARE v_despues JSON;
    SET v_antes = JSON_OBJECT('puesto', OLD.puesto, 'id_sucursal', OLD.id_sucursal, 'estado', OLD.estado, 'fecha_baja', OLD.fecha_baja);
    SET v_despues = JSON_OBJECT('puesto', NEW.puesto, 'id_sucursal', NEW.id_sucursal, 'estado', NEW.estado, 'fecha_baja', NEW.fecha_baja);
    IF v_antes <> v_despues THEN
        INSERT INTO bitacora_auditoria (id_sesion_usuario, username, nombre_tabla, id_registro_afectado, tipo_accion, valores_anteriores, valores_nuevos, fecha_hora)
        VALUES (@id_sesion_usuario, LEFT(COALESCE(CONVERT(@usuario_app USING utf8mb4), CONVERT(SUBSTRING_INDEX(USER(), '@', 1) USING utf8mb4)), 30), 'empleado', NEW.id_empleado, 'UPDATE', v_antes, v_despues, UTC_TIMESTAMP());
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_usuario_ai $$
CREATE TRIGGER trg_usuario_ai AFTER INSERT ON usuario FOR EACH ROW
    INSERT INTO bitacora_auditoria (id_sesion_usuario, username, nombre_tabla, id_registro_afectado, tipo_accion, valores_anteriores, valores_nuevos, fecha_hora)
    VALUES (@id_sesion_usuario, LEFT(COALESCE(CONVERT(@usuario_app USING utf8mb4), CONVERT(SUBSTRING_INDEX(USER(), '@', 1) USING utf8mb4)), 30), 'usuario', NEW.id_usuario, 'INSERT', NULL,
            JSON_OBJECT('username', NEW.username, 'tipo', NEW.tipo, 'id_rol', NEW.id_rol, 'id_cliente', NEW.id_cliente,
                        'id_empleado', NEW.id_empleado, 'estado', NEW.estado, 'requiere_cambio_contrasena', NEW.requiere_cambio_contrasena),
            UTC_TIMESTAMP()) $$

DROP TRIGGER IF EXISTS trg_usuario_au $$
CREATE TRIGGER trg_usuario_au AFTER UPDATE ON usuario FOR EACH ROW
BEGIN
    DECLARE v_antes JSON;
    DECLARE v_despues JSON;
    DECLARE v_cambio_contrasena TINYINT;
    DECLARE v_cambio_clave TINYINT;
    SET v_antes = JSON_OBJECT('username', OLD.username, 'correo', OLD.correo, 'id_rol', OLD.id_rol, 'estado', OLD.estado,
                              'intentos_fallidos', OLD.intentos_fallidos, 'fecha_bloqueo', OLD.fecha_bloqueo,
                              'requiere_cambio_contrasena', OLD.requiere_cambio_contrasena);
    SET v_despues = JSON_OBJECT('username', NEW.username, 'correo', NEW.correo, 'id_rol', NEW.id_rol, 'estado', NEW.estado,
                                'intentos_fallidos', NEW.intentos_fallidos, 'fecha_bloqueo', NEW.fecha_bloqueo,
                                'requiere_cambio_contrasena', NEW.requiere_cambio_contrasena);
    SET v_cambio_contrasena = NOT (OLD.hash_contrasena <=> NEW.hash_contrasena);
    SET v_cambio_clave = NOT (OLD.hash_clave_operacion <=> NEW.hash_clave_operacion);
    IF v_antes <> v_despues OR v_cambio_contrasena = 1 OR v_cambio_clave = 1 THEN
        INSERT INTO bitacora_auditoria (id_sesion_usuario, username, nombre_tabla, id_registro_afectado, tipo_accion, valores_anteriores, valores_nuevos, fecha_hora)
        VALUES (@id_sesion_usuario, LEFT(COALESCE(CONVERT(@usuario_app USING utf8mb4), CONVERT(SUBSTRING_INDEX(USER(), '@', 1) USING utf8mb4)), 30), 'usuario', NEW.id_usuario, 'UPDATE', v_antes,
                JSON_SET(v_despues, '$.contrasena_cambiada', IF(v_cambio_contrasena = 1, 'SI', 'NO'),
                                    '$.clave_operacion_cambiada', IF(v_cambio_clave = 1, 'SI', 'NO')),
                UTC_TIMESTAMP());
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_cuenta_ai $$
CREATE TRIGGER trg_cuenta_ai AFTER INSERT ON cuenta FOR EACH ROW
    INSERT INTO bitacora_auditoria (id_sesion_usuario, username, nombre_tabla, id_registro_afectado, tipo_accion, valores_anteriores, valores_nuevos, fecha_hora)
    VALUES (@id_sesion_usuario, LEFT(COALESCE(CONVERT(@usuario_app USING utf8mb4), CONVERT(SUBSTRING_INDEX(USER(), '@', 1) USING utf8mb4)), 30), 'cuenta', NEW.id_cuenta, 'INSERT', NULL,
            JSON_OBJECT('numero_cuenta', NEW.numero_cuenta, 'id_cliente', NEW.id_cliente, 'id_tipo_cuenta', NEW.id_tipo_cuenta,
                        'id_estado_cuenta', NEW.id_estado_cuenta, 'id_sucursal_apertura', NEW.id_sucursal_apertura,
                        'id_empleado_apertura', NEW.id_empleado_apertura, 'alias', NEW.alias),
            UTC_TIMESTAMP()) $$

DROP TRIGGER IF EXISTS trg_cuenta_au $$
CREATE TRIGGER trg_cuenta_au AFTER UPDATE ON cuenta FOR EACH ROW
BEGIN
    DECLARE v_antes JSON;
    DECLARE v_despues JSON;
    SET v_antes = JSON_OBJECT('saldo_actual', OLD.saldo_actual, 'saldo_retenido', OLD.saldo_retenido, 'id_estado_cuenta', OLD.id_estado_cuenta,
                              'alias', OLD.alias, 'fecha_cierre', OLD.fecha_cierre);
    SET v_despues = JSON_OBJECT('saldo_actual', NEW.saldo_actual, 'saldo_retenido', NEW.saldo_retenido, 'id_estado_cuenta', NEW.id_estado_cuenta,
                                'alias', NEW.alias, 'fecha_cierre', NEW.fecha_cierre);
    IF v_antes <> v_despues THEN
        INSERT INTO bitacora_auditoria (id_sesion_usuario, username, nombre_tabla, id_registro_afectado, tipo_accion, valores_anteriores, valores_nuevos, fecha_hora)
        VALUES (@id_sesion_usuario, LEFT(COALESCE(CONVERT(@usuario_app USING utf8mb4), CONVERT(SUBSTRING_INDEX(USER(), '@', 1) USING utf8mb4)), 30), 'cuenta', NEW.id_cuenta, 'UPDATE', v_antes, v_despues, UTC_TIMESTAMP());
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_transaccion_ai $$
CREATE TRIGGER trg_transaccion_ai AFTER INSERT ON transaccion FOR EACH ROW
    INSERT INTO bitacora_auditoria (id_sesion_usuario, username, nombre_tabla, id_registro_afectado, tipo_accion, valores_anteriores, valores_nuevos, fecha_hora)
    VALUES (@id_sesion_usuario, LEFT(COALESCE(CONVERT(@usuario_app USING utf8mb4), CONVERT(SUBSTRING_INDEX(USER(), '@', 1) USING utf8mb4)), 30), 'transaccion', NEW.id_transaccion, 'INSERT', NULL,
            JSON_OBJECT('numero_transaccion', NEW.numero_transaccion, 'id_tipo_operacion', NEW.id_tipo_operacion,
                        'id_estado_transaccion', NEW.id_estado_transaccion, 'monto', NEW.monto, 'canal', NEW.canal,
                        'referencia', NEW.referencia, 'id_sesion_caja', NEW.id_sesion_caja,
                        'id_transaccion_origen', NEW.id_transaccion_origen),
            UTC_TIMESTAMP()) $$

DROP TRIGGER IF EXISTS trg_transaccion_au $$
CREATE TRIGGER trg_transaccion_au AFTER UPDATE ON transaccion FOR EACH ROW
BEGIN
    IF NEW.id_estado_transaccion <> OLD.id_estado_transaccion THEN
        INSERT INTO bitacora_auditoria (id_sesion_usuario, username, nombre_tabla, id_registro_afectado, tipo_accion, valores_anteriores, valores_nuevos, fecha_hora)
        VALUES (@id_sesion_usuario, LEFT(COALESCE(CONVERT(@usuario_app USING utf8mb4), CONVERT(SUBSTRING_INDEX(USER(), '@', 1) USING utf8mb4)), 30), 'transaccion', NEW.id_transaccion, 'UPDATE',
                JSON_OBJECT('id_estado_transaccion', OLD.id_estado_transaccion),
                JSON_OBJECT('id_estado_transaccion', NEW.id_estado_transaccion),
                UTC_TIMESTAMP());
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_rol_permiso_ai $$
CREATE TRIGGER trg_rol_permiso_ai AFTER INSERT ON rol_permiso FOR EACH ROW
    INSERT INTO bitacora_auditoria (id_sesion_usuario, username, nombre_tabla, id_registro_afectado, tipo_accion, valores_anteriores, valores_nuevos, fecha_hora)
    VALUES (@id_sesion_usuario, LEFT(COALESCE(CONVERT(@usuario_app USING utf8mb4), CONVERT(SUBSTRING_INDEX(USER(), '@', 1) USING utf8mb4)), 30), 'rol_permiso', CONCAT(NEW.id_rol, '-', NEW.id_permiso), 'INSERT', NULL,
            JSON_OBJECT('id_rol', NEW.id_rol, 'id_permiso', NEW.id_permiso, 'id_usuario_asigna', NEW.id_usuario_asigna),
            UTC_TIMESTAMP()) $$

DROP TRIGGER IF EXISTS trg_rol_permiso_ad $$
CREATE TRIGGER trg_rol_permiso_ad AFTER DELETE ON rol_permiso FOR EACH ROW
    INSERT INTO bitacora_auditoria (id_sesion_usuario, username, nombre_tabla, id_registro_afectado, tipo_accion, valores_anteriores, valores_nuevos, fecha_hora)
    VALUES (@id_sesion_usuario, LEFT(COALESCE(CONVERT(@usuario_app USING utf8mb4), CONVERT(SUBSTRING_INDEX(USER(), '@', 1) USING utf8mb4)), 30), 'rol_permiso', CONCAT(OLD.id_rol, '-', OLD.id_permiso), 'DELETE',
            JSON_OBJECT('id_rol', OLD.id_rol, 'id_permiso', OLD.id_permiso, 'id_usuario_asigna', OLD.id_usuario_asigna, 'fecha_asignacion', OLD.fecha_asignacion),
            NULL,
            UTC_TIMESTAMP()) $$

DROP TRIGGER IF EXISTS trg_sesion_caja_ai $$
CREATE TRIGGER trg_sesion_caja_ai AFTER INSERT ON sesion_caja FOR EACH ROW
    INSERT INTO bitacora_auditoria (id_sesion_usuario, username, nombre_tabla, id_registro_afectado, tipo_accion, valores_anteriores, valores_nuevos, fecha_hora)
    VALUES (@id_sesion_usuario, LEFT(COALESCE(CONVERT(@usuario_app USING utf8mb4), CONVERT(SUBSTRING_INDEX(USER(), '@', 1) USING utf8mb4)), 30), 'sesion_caja', NEW.id_sesion_caja, 'INSERT', NULL,
            JSON_OBJECT('id_sucursal', NEW.id_sucursal, 'id_empleado_cajero', NEW.id_empleado_cajero,
                        'monto_apertura', NEW.monto_apertura, 'estado', NEW.estado),
            UTC_TIMESTAMP()) $$

DROP TRIGGER IF EXISTS trg_sesion_caja_au $$
CREATE TRIGGER trg_sesion_caja_au AFTER UPDATE ON sesion_caja FOR EACH ROW
BEGIN
    DECLARE v_antes JSON;
    DECLARE v_despues JSON;
    SET v_antes = JSON_OBJECT('estado', OLD.estado, 'monto_cierre_declarado', OLD.monto_cierre_declarado,
                              'monto_cierre_calculado', OLD.monto_cierre_calculado, 'diferencia', OLD.diferencia,
                              'id_empleado_supervisor', OLD.id_empleado_supervisor, 'observaciones', OLD.observaciones);
    SET v_despues = JSON_OBJECT('estado', NEW.estado, 'monto_cierre_declarado', NEW.monto_cierre_declarado,
                                'monto_cierre_calculado', NEW.monto_cierre_calculado, 'diferencia', NEW.diferencia,
                                'id_empleado_supervisor', NEW.id_empleado_supervisor, 'observaciones', NEW.observaciones);
    IF v_antes <> v_despues THEN
        INSERT INTO bitacora_auditoria (id_sesion_usuario, username, nombre_tabla, id_registro_afectado, tipo_accion, valores_anteriores, valores_nuevos, fecha_hora)
        VALUES (@id_sesion_usuario, LEFT(COALESCE(CONVERT(@usuario_app USING utf8mb4), CONVERT(SUBSTRING_INDEX(USER(), '@', 1) USING utf8mb4)), 30), 'sesion_caja', NEW.id_sesion_caja, 'UPDATE', v_antes, v_despues, UTC_TIMESTAMP());
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_autorizacion_ai $$
CREATE TRIGGER trg_autorizacion_ai AFTER INSERT ON autorizacion FOR EACH ROW
    INSERT INTO bitacora_auditoria (id_sesion_usuario, username, nombre_tabla, id_registro_afectado, tipo_accion, valores_anteriores, valores_nuevos, fecha_hora)
    VALUES (@id_sesion_usuario, LEFT(COALESCE(CONVERT(@usuario_app USING utf8mb4), CONVERT(SUBSTRING_INDEX(USER(), '@', 1) USING utf8mb4)), 30), 'autorizacion', NEW.id_autorizacion, 'INSERT', NULL,
            JSON_OBJECT('id_transaccion', NEW.id_transaccion, 'motivo', NEW.motivo, 'id_empleado_supervisor', NEW.id_empleado_supervisor,
                        'resultado', NEW.resultado, 'comentario', NEW.comentario),
            UTC_TIMESTAMP()) $$

DROP TRIGGER IF EXISTS trg_alerta_ai $$
CREATE TRIGGER trg_alerta_ai AFTER INSERT ON alerta FOR EACH ROW
    INSERT INTO bitacora_auditoria (id_sesion_usuario, username, nombre_tabla, id_registro_afectado, tipo_accion, valores_anteriores, valores_nuevos, fecha_hora)
    VALUES (@id_sesion_usuario, LEFT(COALESCE(CONVERT(@usuario_app USING utf8mb4), CONVERT(SUBSTRING_INDEX(USER(), '@', 1) USING utf8mb4)), 30), 'alerta', NEW.id_alerta, 'INSERT', NULL,
            JSON_OBJECT('id_tipo_alerta', NEW.id_tipo_alerta, 'id_transaccion', NEW.id_transaccion, 'id_cuenta', NEW.id_cuenta,
                        'detalle', NEW.detalle, 'estado', NEW.estado),
            UTC_TIMESTAMP()) $$

DROP TRIGGER IF EXISTS trg_alerta_au $$
CREATE TRIGGER trg_alerta_au AFTER UPDATE ON alerta FOR EACH ROW
BEGIN
    DECLARE v_antes JSON;
    DECLARE v_despues JSON;
    SET v_antes = JSON_OBJECT('estado', OLD.estado, 'id_empleado_revisor', OLD.id_empleado_revisor, 'resolucion', OLD.resolucion);
    SET v_despues = JSON_OBJECT('estado', NEW.estado, 'id_empleado_revisor', NEW.id_empleado_revisor, 'resolucion', NEW.resolucion);
    IF v_antes <> v_despues THEN
        INSERT INTO bitacora_auditoria (id_sesion_usuario, username, nombre_tabla, id_registro_afectado, tipo_accion, valores_anteriores, valores_nuevos, fecha_hora)
        VALUES (@id_sesion_usuario, LEFT(COALESCE(CONVERT(@usuario_app USING utf8mb4), CONVERT(SUBSTRING_INDEX(USER(), '@', 1) USING utf8mb4)), 30), 'alerta', NEW.id_alerta, 'UPDATE', v_antes, v_despues, UTC_TIMESTAMP());
    END IF;
END $$

DELIMITER ;