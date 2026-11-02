USE credi_track;

DELIMITER $$

DROP PROCEDURE IF EXISTS sp_registrar_login $$
CREATE PROCEDURE sp_registrar_login(
    IN p_username VARCHAR(255),
    IN p_exitoso TINYINT,
    IN p_motivo_fallo VARCHAR(40),
    IN p_direccion_ip VARCHAR(45),
    IN p_nombre_equipo VARCHAR(60),
    IN p_max_intentos INT)
BEGIN
    DECLARE v_username VARCHAR(30);
    DECLARE v_id_usuario INT DEFAULT NULL;
    DECLARE v_estado VARCHAR(10) DEFAULT NULL;
    DECLARE v_tipo VARCHAR(10) DEFAULT NULL;
    DECLARE v_intentos INT DEFAULT 0;
    DECLARE v_max INT;
    DECLARE v_motivo VARCHAR(40);
    DECLARE v_token VARCHAR(64) DEFAULT NULL;
    DECLARE v_id_sesion BIGINT DEFAULT NULL;
    DECLARE CONTINUE HANDLER FOR NOT FOUND BEGIN END;

    SET v_username = LEFT(TRIM(COALESCE(p_username, '')), 30);
    SET v_max = IF(p_max_intentos IS NULL OR p_max_intentos < 1, 3, p_max_intentos);

    SELECT id_usuario, estado, tipo, intentos_fallidos
      INTO v_id_usuario, v_estado, v_tipo, v_intentos
      FROM usuario
     WHERE username = v_username;

    SET @usuario_app = v_username;
    SET @id_sesion_usuario = NULL;

    IF p_exitoso = 1 THEN
        IF v_id_usuario IS NULL OR v_estado <> 'ACTIVO' OR v_tipo = 'SISTEMA' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se puede abrir sesión para este usuario';
        END IF;

        IF v_intentos <> 0 THEN
            UPDATE usuario SET intentos_fallidos = 0 WHERE id_usuario = v_id_usuario;
            SET v_intentos = 0;
        END IF;

        INSERT INTO intento_acceso (username_intentado, id_usuario, fecha_hora, exitoso, motivo_fallo, direccion_ip)
        VALUES (v_username, v_id_usuario, UTC_TIMESTAMP(), 1, NULL, p_direccion_ip);

        SET v_token = LOWER(HEX(RANDOM_BYTES(32)));

        INSERT INTO sesion_usuario (id_usuario, identificador_sesion, fecha_inicio, fecha_ultimo_uso, direccion_ip, nombre_equipo)
        VALUES (v_id_usuario, v_token, UTC_TIMESTAMP(), UTC_TIMESTAMP(), p_direccion_ip, p_nombre_equipo);

        SET v_id_sesion = LAST_INSERT_ID();
        SET @id_sesion_usuario = v_id_sesion;
    ELSE
        SET v_motivo = COALESCE(NULLIF(TRIM(p_motivo_fallo), ''), 'CONTRASENA_INCORRECTA');

        INSERT INTO intento_acceso (username_intentado, id_usuario, fecha_hora, exitoso, motivo_fallo, direccion_ip)
        VALUES (v_username, v_id_usuario, UTC_TIMESTAMP(), 0, v_motivo, p_direccion_ip);

        IF v_id_usuario IS NOT NULL AND v_estado = 'ACTIVO' AND v_motivo = 'CONTRASENA_INCORRECTA' THEN
            SET v_intentos = v_intentos + 1;
            IF v_intentos >= v_max THEN
                UPDATE usuario
                   SET intentos_fallidos = v_intentos, estado = 'BLOQUEADO', fecha_bloqueo = UTC_TIMESTAMP()
                 WHERE id_usuario = v_id_usuario;
                UPDATE sesion_usuario
                   SET fecha_fin = UTC_TIMESTAMP(), motivo_cierre = 'BLOQUEO'
                 WHERE id_usuario = v_id_usuario AND fecha_fin IS NULL;
                SET v_estado = 'BLOQUEADO';
            ELSE
                UPDATE usuario SET intentos_fallidos = v_intentos WHERE id_usuario = v_id_usuario;
            END IF;
        END IF;
    END IF;

    SELECT v_id_sesion AS id_sesion_usuario,
           v_token AS token,
           v_id_usuario AS id_usuario,
           v_estado AS estado_usuario,
           IF(v_id_usuario IS NULL, NULL, v_intentos) AS intentos_fallidos,
           IF(v_id_usuario IS NULL, NULL, GREATEST(v_max - v_intentos, 0)) AS intentos_restantes;
END $$

DROP PROCEDURE IF EXISTS sp_validar_sesion $$
CREATE PROCEDURE sp_validar_sesion(
    IN p_token VARCHAR(255),
    IN p_minutos_inactividad INT)
BEGIN
    DECLARE v_id_sesion BIGINT DEFAULT NULL;
    DECLARE v_id_usuario INT DEFAULT NULL;
    DECLARE v_username VARCHAR(30) DEFAULT NULL;
    DECLARE v_fin DATETIME DEFAULT NULL;
    DECLARE v_ultimo_uso DATETIME DEFAULT NULL;
    DECLARE v_estado_usuario VARCHAR(10) DEFAULT NULL;
    DECLARE v_minutos INT;
    DECLARE v_resultado VARCHAR(20);
    DECLARE CONTINUE HANDLER FOR NOT FOUND BEGIN END;

    SET v_minutos = IF(p_minutos_inactividad IS NULL OR p_minutos_inactividad < 1, 15, p_minutos_inactividad);

    SELECT s.id_sesion_usuario, s.id_usuario, u.username, s.fecha_fin, s.fecha_ultimo_uso, u.estado
      INTO v_id_sesion, v_id_usuario, v_username, v_fin, v_ultimo_uso, v_estado_usuario
      FROM sesion_usuario s
      JOIN usuario u ON u.id_usuario = s.id_usuario
     WHERE s.identificador_sesion = p_token;

    IF v_id_sesion IS NULL THEN
        SET v_resultado = 'NO_EXISTE';
    ELSEIF v_fin IS NOT NULL THEN
        SET v_resultado = 'CERRADA';
    ELSEIF v_ultimo_uso < UTC_TIMESTAMP() - INTERVAL v_minutos MINUTE THEN
        UPDATE sesion_usuario SET fecha_fin = UTC_TIMESTAMP(), motivo_cierre = 'EXPIRACION'
         WHERE id_sesion_usuario = v_id_sesion;
        SET v_resultado = 'EXPIRADA';
    ELSEIF v_estado_usuario <> 'ACTIVO' THEN
        UPDATE sesion_usuario
           SET fecha_fin = UTC_TIMESTAMP(), motivo_cierre = IF(v_estado_usuario = 'BLOQUEADO', 'BLOQUEO', 'CIERRE_ADMIN')
         WHERE id_sesion_usuario = v_id_sesion;
        SET v_resultado = 'USUARIO_NO_ACTIVO';
    ELSE
        UPDATE sesion_usuario SET fecha_ultimo_uso = UTC_TIMESTAMP() WHERE id_sesion_usuario = v_id_sesion;
        SET v_resultado = 'VALIDA';
    END IF;

    IF v_resultado = 'VALIDA' THEN
        SET @id_sesion_usuario = v_id_sesion;
        SET @usuario_app = v_username;

        SELECT v_resultado AS resultado,
               s.id_sesion_usuario,
               u.id_usuario,
               u.username,
               u.tipo,
               r.nombre AS rol,
               u.id_cliente,
               u.id_empleado,
               u.requiere_cambio_contrasena,
               CONCAT_WS(' ', i.primer_nombre, i.primer_apellido) AS nombre
          FROM sesion_usuario s
          JOIN usuario u ON u.id_usuario = s.id_usuario
          JOIN rol r ON r.id_rol = u.id_rol
          LEFT JOIN cliente c ON c.id_cliente = u.id_cliente
          LEFT JOIN empleado e ON e.id_empleado = u.id_empleado
          LEFT JOIN identidad i ON i.id_identidad = COALESCE(c.id_identidad, e.id_identidad)
         WHERE s.id_sesion_usuario = v_id_sesion;
    ELSE
        SET @id_sesion_usuario = NULL;
        SET @usuario_app = NULL;

        SELECT v_resultado AS resultado,
               NULL AS id_sesion_usuario,
               NULL AS id_usuario,
               NULL AS username,
               NULL AS tipo,
               NULL AS rol,
               NULL AS id_cliente,
               NULL AS id_empleado,
               NULL AS requiere_cambio_contrasena,
               NULL AS nombre;
    END IF;
END $$

DROP PROCEDURE IF EXISTS sp_cerrar_sesion $$
CREATE PROCEDURE sp_cerrar_sesion(
    IN p_token VARCHAR(255),
    IN p_motivo VARCHAR(30))
BEGIN
    UPDATE sesion_usuario
       SET fecha_fin = UTC_TIMESTAMP(), motivo_cierre = COALESCE(NULLIF(TRIM(p_motivo), ''), 'LOGOUT')
     WHERE identificador_sesion = p_token AND fecha_fin IS NULL;

    SELECT ROW_COUNT() AS sesiones_cerradas;

    SET @id_sesion_usuario = NULL;
    SET @usuario_app = NULL;
END $$

DROP PROCEDURE IF EXISTS sp_consultar_cuentas $$
CREATE PROCEDURE sp_consultar_cuentas(
    IN p_id_sesion BIGINT,
    IN p_id_cliente INT)
BEGIN
    DECLARE v_username VARCHAR(30) DEFAULT NULL;
    DECLARE v_tipo VARCHAR(10) DEFAULT NULL;
    DECLARE v_id_cliente_sesion INT DEFAULT NULL;
    DECLARE v_cantidad INT DEFAULT 0;
    DECLARE CONTINUE HANDLER FOR NOT FOUND BEGIN END;

    SELECT u.username, u.tipo, u.id_cliente
      INTO v_username, v_tipo, v_id_cliente_sesion
      FROM sesion_usuario s
      JOIN usuario u ON u.id_usuario = s.id_usuario
     WHERE s.id_sesion_usuario = p_id_sesion AND s.fecha_fin IS NULL;

    IF v_username IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La sesión no existe o está cerrada';
    END IF;

    IF p_id_cliente IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Debe indicar el cliente';
    END IF;

    IF v_tipo = 'CLIENTE' AND p_id_cliente <> v_id_cliente_sesion THEN
        INSERT INTO bitacora_consulta (id_sesion_usuario, username, tipo_consulta, id_cuenta, id_cliente, criterio, cantidad_registros, fecha_hora)
        VALUES (p_id_sesion, v_username, 'SALDO', NULL, p_id_cliente, 'ACCESO DENEGADO: cuentas de otro cliente', 0, UTC_TIMESTAMP());
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No puede consultar las cuentas de otro cliente';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM cliente WHERE id_cliente = p_id_cliente) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El cliente no existe';
    END IF;

    SELECT COUNT(*) INTO v_cantidad FROM cuenta WHERE id_cliente = p_id_cliente;

    INSERT INTO bitacora_consulta (id_sesion_usuario, username, tipo_consulta, id_cuenta, id_cliente, criterio, cantidad_registros, fecha_hora)
    VALUES (p_id_sesion, v_username, 'SALDO', NULL, p_id_cliente, 'Cuentas y saldos del cliente', v_cantidad, UTC_TIMESTAMP());

    SELECT *
      FROM vw_cuentas_cliente
     WHERE id_cliente = p_id_cliente
     ORDER BY fecha_apertura, numero_cuenta;
END $$

DROP PROCEDURE IF EXISTS sp_movimientos $$
CREATE PROCEDURE sp_movimientos(
    IN p_id_sesion BIGINT,
    IN p_id_cliente INT,
    IN p_id_cuenta INT,
    IN p_desde DATETIME,
    IN p_hasta DATETIME,
    IN p_tipo_operacion VARCHAR(20))
BEGIN
    DECLARE v_username VARCHAR(30) DEFAULT NULL;
    DECLARE v_tipo VARCHAR(10) DEFAULT NULL;
    DECLARE v_id_cliente_sesion INT DEFAULT NULL;
    DECLARE v_id_cliente INT DEFAULT NULL;
    DECLARE v_titular_cuenta INT DEFAULT NULL;
    DECLARE v_tipo_operacion VARCHAR(20) DEFAULT NULL;
    DECLARE v_cantidad INT DEFAULT 0;
    DECLARE CONTINUE HANDLER FOR NOT FOUND BEGIN END;

    SELECT u.username, u.tipo, u.id_cliente
      INTO v_username, v_tipo, v_id_cliente_sesion
      FROM sesion_usuario s
      JOIN usuario u ON u.id_usuario = s.id_usuario
     WHERE s.id_sesion_usuario = p_id_sesion AND s.fecha_fin IS NULL;

    IF v_username IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La sesión no existe o está cerrada';
    END IF;

    IF p_desde IS NOT NULL AND p_hasta IS NOT NULL AND p_desde >= p_hasta THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La fecha inicial debe ser anterior a la fecha final';
    END IF;

    IF p_id_cuenta IS NOT NULL THEN
        SELECT id_cliente INTO v_titular_cuenta FROM cuenta WHERE id_cuenta = p_id_cuenta;
        IF v_titular_cuenta IS NULL THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La cuenta no existe';
        END IF;
    END IF;

    IF v_tipo = 'CLIENTE' THEN
        IF (p_id_cliente IS NOT NULL AND p_id_cliente <> v_id_cliente_sesion)
           OR (v_titular_cuenta IS NOT NULL AND v_titular_cuenta <> v_id_cliente_sesion) THEN
            INSERT INTO bitacora_consulta (id_sesion_usuario, username, tipo_consulta, id_cuenta, id_cliente, criterio, cantidad_registros, fecha_hora)
            VALUES (p_id_sesion, v_username, 'MOVIMIENTOS', p_id_cuenta, COALESCE(v_titular_cuenta, p_id_cliente),
                    'ACCESO DENEGADO: movimientos de otro cliente', 0, UTC_TIMESTAMP());
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No puede consultar los movimientos de otro cliente';
        END IF;
        SET v_id_cliente = v_id_cliente_sesion;
    ELSE
        SET v_id_cliente = p_id_cliente;
    END IF;

    SET v_tipo_operacion = NULLIF(TRIM(p_tipo_operacion), '');

    SELECT COUNT(*) INTO v_cantidad
      FROM vw_movimientos
     WHERE (v_id_cliente IS NULL OR id_cliente = v_id_cliente)
       AND (p_id_cuenta IS NULL OR id_cuenta = p_id_cuenta)
       AND (p_desde IS NULL OR fecha_hora >= p_desde)
       AND (p_hasta IS NULL OR fecha_hora < p_hasta)
       AND (v_tipo_operacion IS NULL OR tipo_operacion = v_tipo_operacion);

    INSERT INTO bitacora_consulta (id_sesion_usuario, username, tipo_consulta, id_cuenta, id_cliente, criterio, cantidad_registros, fecha_hora)
    VALUES (p_id_sesion, v_username, 'MOVIMIENTOS', p_id_cuenta, v_id_cliente,
            LEFT(COALESCE(NULLIF(CONCAT_WS(' | ',
                    CONCAT('desde ', DATE_FORMAT(p_desde, '%Y-%m-%d %H:%i')),
                    CONCAT('hasta ', DATE_FORMAT(p_hasta, '%Y-%m-%d %H:%i')),
                    CONCAT('tipo ', v_tipo_operacion)), ''), 'Sin filtros'), 200),
            v_cantidad, UTC_TIMESTAMP());

    SELECT *
      FROM vw_movimientos
     WHERE (v_id_cliente IS NULL OR id_cliente = v_id_cliente)
       AND (p_id_cuenta IS NULL OR id_cuenta = p_id_cuenta)
       AND (p_desde IS NULL OR fecha_hora >= p_desde)
       AND (p_hasta IS NULL OR fecha_hora < p_hasta)
       AND (v_tipo_operacion IS NULL OR tipo_operacion = v_tipo_operacion)
     ORDER BY fecha_hora DESC, id_movimiento DESC;
END $$

DELIMITER ;
