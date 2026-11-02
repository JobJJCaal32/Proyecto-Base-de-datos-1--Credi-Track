USE credi_track;

CREATE INDEX idx_transaccion_fecha ON transaccion (fecha_hora);
CREATE INDEX idx_intento_acceso_usuario_fecha ON intento_acceso (username_intentado, fecha_hora);
CREATE INDEX idx_intento_acceso_ip_fecha ON intento_acceso (direccion_ip, fecha_hora);
CREATE INDEX idx_bitacora_auditoria_registro ON bitacora_auditoria (nombre_tabla, id_registro_afectado);
CREATE INDEX idx_bitacora_auditoria_fecha ON bitacora_auditoria (fecha_hora);
CREATE INDEX idx_bitacora_consulta_fecha ON bitacora_consulta (fecha_hora);
CREATE INDEX idx_alerta_estado ON alerta (estado);
CREATE INDEX idx_identidad_nombre ON identidad (primer_apellido, primer_nombre);
