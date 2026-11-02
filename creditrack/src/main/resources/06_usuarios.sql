DROP USER IF EXISTS 'ct_backend'@'localhost', 'ct_auditor'@'localhost', 'ct_admin'@'localhost';
DROP ROLE IF EXISTS 'rol_ct_backend', 'rol_ct_auditor', 'rol_ct_admin';

CREATE ROLE 'rol_ct_backend', 'rol_ct_auditor', 'rol_ct_admin';

GRANT SELECT ON credi_track.genero TO 'rol_ct_backend';
GRANT SELECT ON credi_track.departamento TO 'rol_ct_backend';
GRANT SELECT ON credi_track.municipio TO 'rol_ct_backend';
GRANT SELECT ON credi_track.tipo_cuenta TO 'rol_ct_backend';
GRANT SELECT ON credi_track.tipo_operacion TO 'rol_ct_backend';
GRANT SELECT ON credi_track.estado_transaccion TO 'rol_ct_backend';
GRANT SELECT ON credi_track.estado_cuenta TO 'rol_ct_backend';
GRANT SELECT ON credi_track.permiso TO 'rol_ct_backend';
GRANT SELECT ON credi_track.rol TO 'rol_ct_backend';
GRANT SELECT ON credi_track.tipo_alerta TO 'rol_ct_backend';

GRANT SELECT, INSERT, UPDATE ON credi_track.direccion TO 'rol_ct_backend';
GRANT SELECT, INSERT, UPDATE ON credi_track.identidad TO 'rol_ct_backend';
GRANT SELECT, INSERT, UPDATE ON credi_track.cliente TO 'rol_ct_backend';
GRANT SELECT, INSERT, UPDATE ON credi_track.empleado TO 'rol_ct_backend';
GRANT SELECT, INSERT, UPDATE ON credi_track.usuario TO 'rol_ct_backend';
GRANT SELECT, INSERT, UPDATE ON credi_track.sucursal TO 'rol_ct_backend';
GRANT SELECT, INSERT, UPDATE ON credi_track.cuenta TO 'rol_ct_backend';
GRANT SELECT, INSERT, UPDATE ON credi_track.transaccion TO 'rol_ct_backend';
GRANT SELECT, INSERT, UPDATE ON credi_track.sesion_caja TO 'rol_ct_backend';
GRANT SELECT, INSERT, UPDATE ON credi_track.alerta TO 'rol_ct_backend';
GRANT SELECT, INSERT ON credi_track.movimiento_cuenta TO 'rol_ct_backend';
GRANT SELECT, INSERT ON credi_track.autorizacion TO 'rol_ct_backend';
GRANT SELECT, INSERT ON credi_track.estado_cuenta_generado TO 'rol_ct_backend';
GRANT SELECT, INSERT ON credi_track.bitacora_consulta TO 'rol_ct_backend';
GRANT SELECT, INSERT, DELETE ON credi_track.rol_permiso TO 'rol_ct_backend';
GRANT SELECT ON credi_track.sesion_usuario TO 'rol_ct_backend';
GRANT SELECT ON credi_track.intento_acceso TO 'rol_ct_backend';
GRANT SELECT ON credi_track.bitacora_auditoria TO 'rol_ct_backend';

GRANT SELECT ON credi_track.vw_cuentas_cliente TO 'rol_ct_backend';
GRANT SELECT ON credi_track.vw_movimientos TO 'rol_ct_backend';
GRANT SELECT ON credi_track.vw_bitacora_legible TO 'rol_ct_backend';
GRANT SELECT ON credi_track.vw_intentos_fallidos TO 'rol_ct_backend';
GRANT SELECT ON credi_track.vw_operaciones_sucursal TO 'rol_ct_backend';

GRANT EXECUTE ON PROCEDURE credi_track.sp_registrar_login TO 'rol_ct_backend';
GRANT EXECUTE ON PROCEDURE credi_track.sp_validar_sesion TO 'rol_ct_backend';
GRANT EXECUTE ON PROCEDURE credi_track.sp_cerrar_sesion TO 'rol_ct_backend';
GRANT EXECUTE ON PROCEDURE credi_track.sp_consultar_cuentas TO 'rol_ct_backend';
GRANT EXECUTE ON PROCEDURE credi_track.sp_movimientos TO 'rol_ct_backend';

GRANT SELECT ON credi_track.vw_cuentas_cliente TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.vw_movimientos TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.vw_bitacora_legible TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.vw_intentos_fallidos TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.vw_operaciones_sucursal TO 'rol_ct_auditor';

GRANT SELECT (id_usuario, tipo, id_cliente, id_empleado, id_rol, username, requiere_cambio_contrasena,
              fecha_ultimo_cambio, intentos_fallidos, fecha_bloqueo, estado, fecha_creacion, id_usuario_creador)
    ON credi_track.usuario TO 'rol_ct_auditor';
GRANT SELECT (id_identidad, primer_nombre, segundo_nombre, tercer_nombre, primer_apellido, segundo_apellido,
              fecha_nacimiento, id_genero, id_direccion, estado, fecha_creacion)
    ON credi_track.identidad TO 'rol_ct_auditor';

GRANT SELECT ON credi_track.genero TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.departamento TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.municipio TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.tipo_cuenta TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.tipo_operacion TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.estado_transaccion TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.estado_cuenta TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.permiso TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.rol TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.rol_permiso TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.tipo_alerta TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.sucursal TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.cliente TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.empleado TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.cuenta TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.transaccion TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.movimiento_cuenta TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.autorizacion TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.sesion_caja TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.alerta TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.estado_cuenta_generado TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.sesion_usuario TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.intento_acceso TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.bitacora_auditoria TO 'rol_ct_auditor';
GRANT SELECT ON credi_track.bitacora_consulta TO 'rol_ct_auditor';

GRANT ALL PRIVILEGES ON credi_track.* TO 'rol_ct_admin';

CREATE USER 'ct_backend'@'localhost' IDENTIFIED BY 'CtBackend#2026x' FAILED_LOGIN_ATTEMPTS 5 PASSWORD_LOCK_TIME 1;
CREATE USER 'ct_auditor'@'localhost' IDENTIFIED BY 'CtAuditor#2026x' FAILED_LOGIN_ATTEMPTS 5 PASSWORD_LOCK_TIME 1;
CREATE USER 'ct_admin'@'localhost' IDENTIFIED BY 'CtAdmin#2026x' FAILED_LOGIN_ATTEMPTS 5 PASSWORD_LOCK_TIME 1;

GRANT 'rol_ct_backend' TO 'ct_backend'@'localhost';
GRANT 'rol_ct_auditor' TO 'ct_auditor'@'localhost';
GRANT 'rol_ct_admin' TO 'ct_admin'@'localhost';

SET DEFAULT ROLE ALL TO 'ct_backend'@'localhost', 'ct_auditor'@'localhost', 'ct_admin'@'localhost';
