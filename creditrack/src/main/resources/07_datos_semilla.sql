USE credi_track;

SET @clave_aes = 'CrediTrack#2026!';
SET @usuario_app = 'sistema';
SET @id_sesion_usuario = NULL;
SET @permitir_cambio_saldo = 0;

INSERT IGNORE INTO genero (nombre) VALUES ('Masculino'), ('Femenino');

INSERT IGNORE INTO departamento (nombre) VALUES ('Alta Verapaz');

INSERT IGNORE INTO municipio (id_departamento, nombre)
SELECT id_departamento, 'Cobán' FROM departamento WHERE nombre = 'Alta Verapaz';

INSERT IGNORE INTO municipio (id_departamento, nombre)
SELECT id_departamento, 'San Pedro Carchá' FROM departamento WHERE nombre = 'Alta Verapaz';

SET @mun_coban = (SELECT m.id_municipio FROM municipio m JOIN departamento d ON d.id_departamento = m.id_departamento
                   WHERE d.nombre = 'Alta Verapaz' AND m.nombre = 'Cobán');
SET @mun_carcha = (SELECT m.id_municipio FROM municipio m JOIN departamento d ON d.id_departamento = m.id_departamento
                    WHERE d.nombre = 'Alta Verapaz' AND m.nombre = 'San Pedro Carchá');
SET @g_m = (SELECT id_genero FROM genero WHERE nombre = 'Masculino');
SET @g_f = (SELECT id_genero FROM genero WHERE nombre = 'Femenino');

INSERT INTO tipo_cuenta (nombre, descripcion, monto_apertura_minimo) VALUES
    ('MONETARIA', 'Cuenta monetaria', 100.00),
    ('AHORRO', 'Cuenta de ahorro', 200.00);

INSERT INTO estado_cuenta (nombre, descripcion, permite_operar) VALUES
    ('ACTIVA', 'Cuenta activa, opera normalmente', 1),
    ('BLOQUEADA', 'Cuenta bloqueada temporalmente', 0),
    ('INACTIVA', 'Cuenta sin movimientos por mucho tiempo', 0),
    ('CERRADA', 'Cuenta cerrada definitivamente', 0);

INSERT INTO tipo_operacion (nombre, descripcion, afecta_efectivo) VALUES
    ('DEPOSITO', 'Depósito de efectivo', 1),
    ('RETIRO', 'Retiro de efectivo', 1),
    ('TRANSFERENCIA', 'Transferencia entre cuentas', 0);

INSERT INTO estado_transaccion (nombre, descripcion) VALUES
    ('PENDIENTE', 'Registrada, en espera de aplicarse'),
    ('APLICADA', 'Aplicada a los saldos de las cuentas'),
    ('RECHAZADA', 'No se aplicó'),
    ('REVERSADA', 'Aplicada y luego anulada por un reverso');

INSERT INTO tipo_alerta (nombre, descripcion, nivel_riesgo, umbral_monto, umbral_cantidad, ventana_minutos) VALUES
    ('MONTO_ALTO', 'Una sola operación igual o mayor al umbral', 'ALTO', 25000.00, NULL, NULL),
    ('TRANSACCIONES_FRECUENTES', 'Varios cargos a la misma cuenta dentro de la ventana de tiempo', 'MEDIO', NULL, 5, 10);

INSERT INTO rol (nombre, descripcion, tipo_usuario) VALUES
    ('SISTEMA', 'Usuario técnico de arranque, no inicia sesión', 'SISTEMA'),
    ('CLIENTE', 'Cliente con acceso a la banca virtual', 'CLIENTE'),
    ('ADMINISTRADOR', 'Gestiona personal, usuarios y permisos', 'EMPLEADO'),
    ('OFICIAL_CUENTAS', 'Registra clientes y abre cuentas', 'EMPLEADO'),
    ('CAJERO', 'Atiende ventanilla', 'EMPLEADO'),
    ('SUPERVISOR', 'Autoriza operaciones, arquea cajas y revisa alertas', 'EMPLEADO'),
    ('AUDITOR', 'Consulta bitácoras y reportes', 'EMPLEADO'),
    ('GERENTE', 'Supervisa la agencia y consulta reportes', 'EMPLEADO');

INSERT INTO permiso (nombre, descripcion) VALUES
    ('CONSULTAR_CUENTAS', 'Consultar cuentas y saldos'),
    ('CONSULTAR_MOVIMIENTOS', 'Consultar movimientos de cuentas'),
    ('TRANSFERIR', 'Transferir entre cuentas'),
    ('GENERAR_ESTADO_CUENTA', 'Generar estados de cuenta'),
    ('CAMBIAR_CONTRASENA', 'Cambiar la propia contraseña'),
    ('CONFIGURAR_CLAVE_OPERACION', 'Configurar la clave de operación'),
    ('GESTIONAR_ALIAS', 'Ponerle alias a una cuenta'),
    ('VER_ACCESOS', 'Ver los últimos accesos propios'),
    ('REGISTRAR_CLIENTE', 'Registrar clientes nuevos'),
    ('ABRIR_CUENTA', 'Abrir cuentas bancarias'),
    ('CREAR_USUARIO_CLIENTE', 'Crear el usuario de banca virtual de un cliente'),
    ('BUSCAR_CLIENTE', 'Buscar clientes por DPI o código'),
    ('ACTUALIZAR_CLIENTE', 'Actualizar datos de un cliente'),
    ('CAMBIAR_ESTADO_CUENTA', 'Bloquear, desbloquear o cerrar cuentas'),
    ('DAR_BAJA_CLIENTE', 'Dar de baja a un cliente'),
    ('REALIZAR_DEPOSITO', 'Recibir depósitos de efectivo'),
    ('REALIZAR_RETIRO', 'Entregar retiros de efectivo'),
    ('ABRIR_CAJA', 'Abrir una sesión de caja'),
    ('CERRAR_CAJA', 'Cerrar la propia sesión de caja'),
    ('TRANSFERIR_VENTANILLA', 'Transferir entre cuentas en ventanilla'),
    ('ARQUEAR_CAJA', 'Arquear cajas de otros cajeros'),
    ('AUTORIZAR_OPERACION', 'Autorizar operaciones sobre el límite'),
    ('REVERSAR_TRANSACCION', 'Reversar transferencias'),
    ('GESTIONAR_ALERTAS', 'Revisar y resolver alertas'),
    ('REGISTRAR_EMPLEADO', 'Registrar empleados y crear su usuario'),
    ('GESTIONAR_EMPLEADOS', 'Cambiar puesto, sucursal o dar de baja a empleados'),
    ('DESBLOQUEAR_USUARIO', 'Desbloquear usuarios'),
    ('RESETEAR_CONTRASENA', 'Resetear contraseñas de usuarios'),
    ('ASIGNAR_PERMISOS', 'Asignar permisos a roles'),
    ('GESTIONAR_SUCURSALES', 'Crear sucursales y asignar responsables'),
    ('CONSULTAR_BITACORA', 'Consultar la bitácora de auditoría'),
    ('CONSULTAR_INTENTOS_ACCESO', 'Consultar intentos de acceso fallidos'),
    ('CONSULTAR_REPORTES', 'Consultar reportes de operaciones');

INSERT INTO usuario (tipo, id_rol, username, requiere_cambio_contrasena)
VALUES ('SISTEMA', (SELECT id_rol FROM rol WHERE nombre = 'SISTEMA'), 'sistema', 0);
SET @u_sistema = LAST_INSERT_ID();

INSERT INTO rol_permiso (id_rol, id_permiso, id_usuario_asigna)
SELECT r.id_rol, p.id_permiso, @u_sistema
  FROM rol r
  JOIN permiso p ON
       (r.nombre = 'CLIENTE' AND p.nombre IN ('CONSULTAR_CUENTAS', 'CONSULTAR_MOVIMIENTOS', 'TRANSFERIR', 'GENERAR_ESTADO_CUENTA',
                                              'CAMBIAR_CONTRASENA', 'CONFIGURAR_CLAVE_OPERACION', 'GESTIONAR_ALIAS', 'VER_ACCESOS'))
    OR (r.nombre = 'OFICIAL_CUENTAS' AND p.nombre IN ('REGISTRAR_CLIENTE', 'ABRIR_CUENTA', 'CREAR_USUARIO_CLIENTE', 'BUSCAR_CLIENTE',
                                                      'ACTUALIZAR_CLIENTE', 'CAMBIAR_ESTADO_CUENTA', 'DAR_BAJA_CLIENTE',
                                                      'CONSULTAR_CUENTAS', 'CONSULTAR_MOVIMIENTOS', 'CAMBIAR_CONTRASENA'))
    OR (r.nombre = 'CAJERO' AND p.nombre IN ('REALIZAR_DEPOSITO', 'REALIZAR_RETIRO', 'ABRIR_CAJA', 'CERRAR_CAJA',
                                             'TRANSFERIR_VENTANILLA', 'BUSCAR_CLIENTE', 'CONSULTAR_CUENTAS', 'CAMBIAR_CONTRASENA'))
    OR (r.nombre = 'SUPERVISOR' AND p.nombre IN ('ARQUEAR_CAJA', 'AUTORIZAR_OPERACION', 'REVERSAR_TRANSACCION', 'GESTIONAR_ALERTAS',
                                                 'BUSCAR_CLIENTE', 'CONSULTAR_CUENTAS', 'CONSULTAR_MOVIMIENTOS', 'CAMBIAR_CONTRASENA'))
    OR (r.nombre = 'ADMINISTRADOR' AND p.nombre IN ('REGISTRAR_EMPLEADO', 'GESTIONAR_EMPLEADOS', 'DESBLOQUEAR_USUARIO',
                                                    'RESETEAR_CONTRASENA', 'ASIGNAR_PERMISOS', 'GESTIONAR_SUCURSALES', 'CAMBIAR_CONTRASENA'))
    OR (r.nombre = 'AUDITOR' AND p.nombre IN ('CONSULTAR_BITACORA', 'CONSULTAR_INTENTOS_ACCESO', 'CONSULTAR_REPORTES',
                                              'CONSULTAR_MOVIMIENTOS', 'CAMBIAR_CONTRASENA'))
    OR (r.nombre = 'GERENTE' AND p.nombre IN ('CONSULTAR_REPORTES', 'CONSULTAR_BITACORA', 'AUTORIZAR_OPERACION',
                                              'GESTIONAR_ALERTAS', 'CONSULTAR_MOVIMIENTOS', 'CAMBIAR_CONTRASENA'));

INSERT INTO sucursal (codigo_sucursal, nombre, id_municipio, direccion, telefono, fecha_apertura)
VALUES ('AG-001', 'Agencia Central Cobán', @mun_coban, '1a. Calle 3-40, Zona 1, Cobán', '7951-0001', '2020-01-15');
SET @suc = LAST_INSERT_ID();

INSERT INTO direccion (id_municipio, descripcion_direccion) VALUES (@mun_coban, '3a. Calle 2-15, Zona 2, Cobán');
INSERT INTO identidad (dpi, nit, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, fecha_nacimiento, id_genero, telefono, id_direccion)
VALUES (HEX(AES_ENCRYPT('2501123451601', @clave_aes)), '1234567-8', 'Luis', 'Alberto', 'Pop', 'Xol', '1985-03-12', @g_m,
        HEX(AES_ENCRYPT('5512-3401', @clave_aes)), LAST_INSERT_ID());
INSERT INTO empleado (id_identidad, codigo_empleado, id_sucursal, puesto, fecha_ingreso, id_usuario_registro, tipo_registro)
VALUES (LAST_INSERT_ID(), 'EMP-0001', @suc, 'Administrador de sistemas', '2020-01-15', @u_sistema, 'SISTEMA');
SET @e_admin = LAST_INSERT_ID();
INSERT INTO usuario (tipo, id_empleado, id_rol, username, correo, hash_contrasena, requiere_cambio_contrasena, fecha_ultimo_cambio, id_usuario_creador)
VALUES ('EMPLEADO', @e_admin, (SELECT id_rol FROM rol WHERE nombre = 'ADMINISTRADOR'), 'lpop.admin', 'lpop@creditrack.gt',
        '$2a$10$bPZ6JSST8hDA3RA7lNa5puKHXGnUMznjOb4A0qw2M/psQo0SagY8G', 0, UTC_TIMESTAMP(), @u_sistema);
SET @u_admin = LAST_INSERT_ID();

SET @usuario_app = 'lpop.admin';

INSERT INTO direccion (id_municipio, descripcion_direccion) VALUES (@mun_coban, '7a. Calle 4-22, Zona 4, Cobán');
INSERT INTO identidad (dpi, nit, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, fecha_nacimiento, id_genero, telefono, id_direccion)
VALUES (HEX(AES_ENCRYPT('2504456784912', @clave_aes)), '4567890-1', 'María', 'José', 'Choc', 'Cú', '1990-01-30', @g_f,
        HEX(AES_ENCRYPT('5512-3404', @clave_aes)), LAST_INSERT_ID());
INSERT INTO empleado (id_identidad, codigo_empleado, id_sucursal, puesto, fecha_ingreso, id_usuario_registro, tipo_registro)
VALUES (LAST_INSERT_ID(), 'EMP-0002', @suc, 'Oficial de cuentas', '2021-05-10', @u_admin, 'EMPLEADO');
SET @e_oficial = LAST_INSERT_ID();
INSERT INTO usuario (tipo, id_empleado, id_rol, username, correo, hash_contrasena, requiere_cambio_contrasena, fecha_ultimo_cambio, id_usuario_creador)
VALUES ('EMPLEADO', @e_oficial, (SELECT id_rol FROM rol WHERE nombre = 'OFICIAL_CUENTAS'), 'mchoc.oficial', 'mchoc@creditrack.gt',
        '$2a$10$Gh2CJ.uQePS5aCkXO1yBo..JS0wZzg2ZF947zWE2SfgVGe3z57nni', 0, UTC_TIMESTAMP(), @u_admin);
SET @u_oficial = LAST_INSERT_ID();

INSERT INTO direccion (id_municipio, descripcion_direccion) VALUES (@mun_coban, '6a. Avenida 10-50, Zona 1, Cobán');
INSERT INTO identidad (dpi, nit, primer_nombre, primer_apellido, segundo_apellido, fecha_nacimiento, id_genero, telefono, id_direccion)
VALUES (HEX(AES_ENCRYPT('2505567895023', @clave_aes)), '5678901-2', 'Pedro', 'Xol', 'Bol', '1988-09-14', @g_m,
        HEX(AES_ENCRYPT('5512-3405', @clave_aes)), LAST_INSERT_ID());
INSERT INTO empleado (id_identidad, codigo_empleado, id_sucursal, puesto, fecha_ingreso, id_usuario_registro, tipo_registro)
VALUES (LAST_INSERT_ID(), 'EMP-0003', @suc, 'Auditor interno', '2021-08-16', @u_admin, 'EMPLEADO');
SET @e_auditor = LAST_INSERT_ID();
INSERT INTO usuario (tipo, id_empleado, id_rol, username, correo, hash_contrasena, requiere_cambio_contrasena, fecha_ultimo_cambio, id_usuario_creador)
VALUES ('EMPLEADO', @e_auditor, (SELECT id_rol FROM rol WHERE nombre = 'AUDITOR'), 'pxol.auditor', 'pxol@creditrack.gt',
        '$2a$10$x3o5TSk1B/uRycbXES9uKOjgimtjF.AFWIj7s1FjawOJ1I0tc0ZQu', 0, UTC_TIMESTAMP(), @u_admin);

INSERT INTO direccion (id_municipio, descripcion_direccion) VALUES (@mun_coban, '5a. Avenida 1-08, Zona 3, Cobán');
INSERT INTO identidad (dpi, nit, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, fecha_nacimiento, id_genero, telefono, id_direccion)
VALUES (HEX(AES_ENCRYPT('2502234562716', @clave_aes)), '2345678-9', 'Ana', 'Lucía', 'Caal', 'Choc', '1995-07-21', @g_f,
        HEX(AES_ENCRYPT('5512-3402', @clave_aes)), LAST_INSERT_ID());
INSERT INTO empleado (id_identidad, codigo_empleado, id_sucursal, puesto, fecha_ingreso, id_usuario_registro, tipo_registro)
VALUES (LAST_INSERT_ID(), 'EMP-0004', @suc, 'Cajera', '2022-02-01', @u_admin, 'EMPLEADO');
SET @e_cajero = LAST_INSERT_ID();
INSERT INTO usuario (tipo, id_empleado, id_rol, username, correo, hash_contrasena, requiere_cambio_contrasena, fecha_ultimo_cambio, id_usuario_creador)
VALUES ('EMPLEADO', @e_cajero, (SELECT id_rol FROM rol WHERE nombre = 'CAJERO'), 'acaal.caja', 'acaal@creditrack.gt',
        '$2a$10$wJ7rVA87YZZdeSJWGxUr/ugkPknunrxkW0HghNDw.xTaaP7z3yDBe', 0, UTC_TIMESTAMP(), @u_admin);

INSERT INTO direccion (id_municipio, descripcion_direccion) VALUES (@mun_carcha, 'Barrio San Vicente, San Pedro Carchá');
INSERT INTO identidad (dpi, nit, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, fecha_nacimiento, id_genero, telefono, id_direccion)
VALUES (HEX(AES_ENCRYPT('2503345673809', @clave_aes)), '3456789-0', 'Carlos', 'Enrique', 'Tiul', 'Maaz', '1980-11-02', @g_m,
        HEX(AES_ENCRYPT('5512-3403', @clave_aes)), LAST_INSERT_ID());
INSERT INTO empleado (id_identidad, codigo_empleado, id_sucursal, puesto, fecha_ingreso, id_usuario_registro, tipo_registro)
VALUES (LAST_INSERT_ID(), 'EMP-0005', @suc, 'Supervisor de agencia', '2020-03-01', @u_admin, 'EMPLEADO');
SET @e_supervisor = LAST_INSERT_ID();
INSERT INTO usuario (tipo, id_empleado, id_rol, username, correo, hash_contrasena, requiere_cambio_contrasena, fecha_ultimo_cambio, id_usuario_creador)
VALUES ('EMPLEADO', @e_supervisor, (SELECT id_rol FROM rol WHERE nombre = 'SUPERVISOR'), 'ctiul.super', 'ctiul@creditrack.gt',
        '$2a$10$DW9JnW7biE7Hx3pLLYSGNOzwZgavC3K8ujnxK8gs/rKQ5t04QF4f.', 0, UTC_TIMESTAMP(), @u_admin);

UPDATE sucursal SET id_empleado_responsable = @e_supervisor WHERE id_sucursal = @suc;

CALL sp_registrar_login('mchoc.oficial', 1, NULL, '127.0.0.1', 'CARGA_INICIAL', 3);
SET @s_oficial = @id_sesion_usuario;

SET @tc_monetaria = (SELECT id_tipo_cuenta FROM tipo_cuenta WHERE nombre = 'MONETARIA');
SET @tc_ahorro = (SELECT id_tipo_cuenta FROM tipo_cuenta WHERE nombre = 'AHORRO');
SET @ec_activa = (SELECT id_estado_cuenta FROM estado_cuenta WHERE nombre = 'ACTIVA');
SET @op_deposito = (SELECT id_tipo_operacion FROM tipo_operacion WHERE nombre = 'DEPOSITO');
SET @op_transferencia = (SELECT id_tipo_operacion FROM tipo_operacion WHERE nombre = 'TRANSFERENCIA');
SET @et_aplicada = (SELECT id_estado_transaccion FROM estado_transaccion WHERE nombre = 'APLICADA');
SET @rol_cliente = (SELECT id_rol FROM rol WHERE nombre = 'CLIENTE');

START TRANSACTION;
INSERT INTO direccion (id_municipio, descripcion_direccion) VALUES (@mun_coban, 'Barrio San Marcos, Zona 5, Cobán');
INSERT INTO identidad (dpi, nit, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, fecha_nacimiento, id_genero, telefono, id_direccion)
VALUES (HEX(AES_ENCRYPT('2508890128356', @clave_aes)), '8901234-5', 'Marta', 'Elena', 'Coc', 'Sam', '1992-04-18', @g_f,
        HEX(AES_ENCRYPT('5512-3408', @clave_aes)), LAST_INSERT_ID());
INSERT INTO cliente (id_identidad, codigo_cliente, fecha_alta, id_usuario_registro, tipo_registro)
VALUES (LAST_INSERT_ID(), 'CLI-000001', UTC_DATE(), @u_oficial, 'EMPLEADO');
SET @c_marta = LAST_INSERT_ID();
INSERT INTO cuenta (numero_cuenta, id_cliente, id_tipo_cuenta, id_estado_cuenta, id_sucursal_apertura, fecha_apertura, id_empleado_apertura, alias)
VALUES ('1000000001', @c_marta, @tc_monetaria, @ec_activa, @suc, UTC_DATE(), @e_oficial, NULL);
SET @cta_marta = LAST_INSERT_ID();
INSERT INTO transaccion (numero_transaccion, id_tipo_operacion, id_estado_transaccion, monto, fecha_hora, canal, id_sesion_usuario, concepto, referencia, identidad_verificada)
VALUES ('TRX-0000000001', @op_deposito, @et_aplicada, 5000.00, UTC_TIMESTAMP(), 'APERTURA', @s_oficial, 'Depósito de apertura', 'APERTURA-1000000001', 1);
INSERT INTO movimiento_cuenta (id_transaccion, id_cuenta, naturaleza, monto, saldo_anterior, saldo_posterior, orden)
VALUES (LAST_INSERT_ID(), @cta_marta, 'ABONO', 5000.00, 0.00, 5000.00, 1);
SET @permitir_cambio_saldo = 1;
UPDATE cuenta SET saldo_actual = saldo_actual + 5000.00 WHERE id_cuenta = @cta_marta;
SET @permitir_cambio_saldo = 0;
INSERT INTO usuario (tipo, id_cliente, id_rol, username, correo, hash_contrasena, requiere_cambio_contrasena, fecha_ultimo_cambio, id_usuario_creador)
VALUES ('CLIENTE', @c_marta, @rol_cliente, 'marta.coc', 'marta.coc@correo.gt', '$2a$10$W/zoKfXZrGyQ3xbNiDDH1ehFVVRiZaCBNmunX/EuYyRREaWZDuYQu', 0, UTC_TIMESTAMP(), @u_oficial);
COMMIT;

START TRANSACTION;
INSERT INTO direccion (id_municipio, descripcion_direccion) VALUES (@mun_coban, '1a. Calle 8-30, Zona 1, Cobán');
INSERT INTO identidad (dpi, nit, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, fecha_nacimiento, id_genero, telefono, id_direccion)
VALUES (HEX(AES_ENCRYPT('2509901239467', @clave_aes)), '9012345-6', 'José', 'Manuel', 'Ical', 'Pop', '1987-08-09', @g_m,
        HEX(AES_ENCRYPT('5512-3409', @clave_aes)), LAST_INSERT_ID());
INSERT INTO cliente (id_identidad, codigo_cliente, fecha_alta, id_usuario_registro, tipo_registro)
VALUES (LAST_INSERT_ID(), 'CLI-000002', UTC_DATE(), @u_oficial, 'EMPLEADO');
SET @c_jose = LAST_INSERT_ID();
INSERT INTO cuenta (numero_cuenta, id_cliente, id_tipo_cuenta, id_estado_cuenta, id_sucursal_apertura, fecha_apertura, id_empleado_apertura, alias)
VALUES ('1000000002', @c_jose, @tc_ahorro, @ec_activa, @suc, UTC_DATE(), @e_oficial, NULL);
SET @cta_jose = LAST_INSERT_ID();
INSERT INTO transaccion (numero_transaccion, id_tipo_operacion, id_estado_transaccion, monto, fecha_hora, canal, id_sesion_usuario, concepto, referencia, identidad_verificada)
VALUES ('TRX-0000000002', @op_deposito, @et_aplicada, 1500.00, UTC_TIMESTAMP(), 'APERTURA', @s_oficial, 'Depósito de apertura', 'APERTURA-1000000002', 1);
INSERT INTO movimiento_cuenta (id_transaccion, id_cuenta, naturaleza, monto, saldo_anterior, saldo_posterior, orden)
VALUES (LAST_INSERT_ID(), @cta_jose, 'ABONO', 1500.00, 0.00, 1500.00, 1);
SET @permitir_cambio_saldo = 1;
UPDATE cuenta SET saldo_actual = saldo_actual + 1500.00 WHERE id_cuenta = @cta_jose;
SET @permitir_cambio_saldo = 0;
INSERT INTO usuario (tipo, id_cliente, id_rol, username, correo, hash_contrasena, requiere_cambio_contrasena, fecha_ultimo_cambio, id_usuario_creador)
VALUES ('CLIENTE', @c_jose, @rol_cliente, 'jose.ical', 'jose.ical@correo.gt', '$2a$10$G2w6lo6B91TNBG8OuKIQ6ubQwk99nrqr7AL1eqXFJ45YPO2bhUPfy', 0, UTC_TIMESTAMP(), @u_oficial);
COMMIT;

SET @token_oficial = (SELECT identificador_sesion FROM sesion_usuario WHERE id_sesion_usuario = @s_oficial);
CALL sp_cerrar_sesion(@token_oficial, 'LOGOUT');

CALL sp_registrar_login('marta.coc', 1, NULL, '10.0.0.8', 'CARGA_INICIAL', 3);
SET @s_marta = @id_sesion_usuario;

START TRANSACTION;
SELECT saldo_actual INTO @saldo_origen FROM cuenta WHERE id_cuenta = @cta_marta FOR UPDATE;
SELECT saldo_actual INTO @saldo_destino FROM cuenta WHERE id_cuenta = @cta_jose FOR UPDATE;
INSERT INTO transaccion (numero_transaccion, id_tipo_operacion, id_estado_transaccion, monto, fecha_hora, canal, id_sesion_usuario, concepto, referencia)
VALUES ('TRX-0000000003', @op_transferencia, @et_aplicada, 750.00, UTC_TIMESTAMP(), 'VIRTUAL', @s_marta, 'Pago de renta', 'SEMILLA-TRF-0001');
SET @tx = LAST_INSERT_ID();
INSERT INTO movimiento_cuenta (id_transaccion, id_cuenta, naturaleza, monto, saldo_anterior, saldo_posterior, orden)
VALUES (@tx, @cta_marta, 'CARGO', 750.00, @saldo_origen, @saldo_origen - 750.00, 1),
       (@tx, @cta_jose, 'ABONO', 750.00, @saldo_destino, @saldo_destino + 750.00, 2);
SET @permitir_cambio_saldo = 1;
UPDATE cuenta SET saldo_actual = saldo_actual - 750.00 WHERE id_cuenta = @cta_marta;
UPDATE cuenta SET saldo_actual = saldo_actual + 750.00 WHERE id_cuenta = @cta_jose;
SET @permitir_cambio_saldo = 0;
COMMIT;

SET @token_marta = (SELECT identificador_sesion FROM sesion_usuario WHERE id_sesion_usuario = @s_marta);
CALL sp_cerrar_sesion(@token_marta, 'LOGOUT');

SET @usuario_app = NULL;
SET @id_sesion_usuario = NULL;
SET @permitir_cambio_saldo = 0;
