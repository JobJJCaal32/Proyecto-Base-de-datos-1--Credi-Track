CREATE DATABASE IF NOT EXISTS credi_track
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_spanish_ci;

USE credi_track;

CREATE TABLE IF NOT EXISTS genero(
    id_genero INT AUTO_INCREMENT,
    nombre VARCHAR(15) NOT NULL,
    estado VARCHAR(8) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT pk_genero PRIMARY KEY (id_genero),
    CONSTRAINT uq_genero_nombre UNIQUE (nombre),
    CONSTRAINT chk_genero_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS departamento(
    id_departamento INT AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL,
    CONSTRAINT pk_departamento PRIMARY KEY (id_departamento),
    CONSTRAINT uq_departamento_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS tipo_cuenta(
    id_tipo_cuenta INT AUTO_INCREMENT,
    nombre VARCHAR(20) NOT NULL,
    descripcion VARCHAR(100) NOT NULL,
    monto_apertura_minimo DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT pk_tipo_cuenta PRIMARY KEY (id_tipo_cuenta),
    CONSTRAINT uq_tipo_cuenta_nombre UNIQUE (nombre),
    CONSTRAINT chk_tipo_cuenta_apertura CHECK (monto_apertura_minimo >= 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS tipo_operacion(
    id_tipo_operacion INT AUTO_INCREMENT,
    nombre VARCHAR(20) NOT NULL,
    descripcion VARCHAR(100) NOT NULL,
    afecta_efectivo TINYINT(1) NOT NULL,
    CONSTRAINT pk_tipo_operacion PRIMARY KEY (id_tipo_operacion),
    CONSTRAINT uq_tipo_operacion_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS estado_transaccion(
    id_estado_transaccion INT AUTO_INCREMENT,
    nombre VARCHAR(20) NOT NULL,
    descripcion VARCHAR(100) NOT NULL,
    CONSTRAINT pk_estado_transaccion PRIMARY KEY (id_estado_transaccion),
    CONSTRAINT uq_estado_transaccion_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS estado_cuenta(
    id_estado_cuenta INT AUTO_INCREMENT,
    nombre VARCHAR(20) NOT NULL,
    descripcion VARCHAR(100) NOT NULL,
    permite_operar TINYINT(1) NOT NULL,
    CONSTRAINT pk_estado_cuenta PRIMARY KEY (id_estado_cuenta),
    CONSTRAINT uq_estado_cuenta_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS permiso(
    id_permiso INT AUTO_INCREMENT,
    nombre VARCHAR(40) NOT NULL,
    descripcion VARCHAR(150) NOT NULL,
    CONSTRAINT pk_permiso PRIMARY KEY (id_permiso),
    CONSTRAINT uq_permiso_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS rol(
    id_rol INT AUTO_INCREMENT,
    nombre VARCHAR(60) NOT NULL,
    descripcion VARCHAR(200) NULL,
    tipo_usuario VARCHAR(10) NOT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT pk_rol PRIMARY KEY (id_rol),
    CONSTRAINT uq_rol_nombre UNIQUE (nombre),
    CONSTRAINT uq_rol_tipo_usuario UNIQUE (id_rol, tipo_usuario),
    CONSTRAINT chk_rol_tipo_usuario CHECK (tipo_usuario IN ('CLIENTE','EMPLEADO','SISTEMA'))
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS tipo_alerta(
    id_tipo_alerta INT AUTO_INCREMENT,
    nombre VARCHAR(80) NOT NULL,
    descripcion VARCHAR(200) NULL,
    nivel_riesgo VARCHAR(10) NOT NULL,
    umbral_monto DECIMAL(15,2) NULL,
    umbral_cantidad INT NULL,
    ventana_minutos INT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT pk_tipo_alerta PRIMARY KEY (id_tipo_alerta),
    CONSTRAINT uq_tipo_alerta_nombre UNIQUE (nombre),
    CONSTRAINT chk_tipo_alerta_nivel CHECK (nivel_riesgo IN ('BAJO','MEDIO','ALTO')),
    CONSTRAINT chk_tipo_alerta_umbral_monto CHECK (umbral_monto IS NULL OR umbral_monto > 0),
    CONSTRAINT chk_tipo_alerta_umbral CHECK (umbral_monto IS NOT NULL OR umbral_cantidad IS NOT NULL)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS municipio(
    id_municipio INT AUTO_INCREMENT,
    id_departamento INT NOT NULL,
    nombre VARCHAR(60) NOT NULL,
    CONSTRAINT pk_municipio PRIMARY KEY (id_municipio),
    CONSTRAINT fk_municipio_departamento FOREIGN KEY (id_departamento) REFERENCES departamento (id_departamento) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT uq_municipio_departamento_nombre UNIQUE (id_departamento, nombre)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS direccion(
    id_direccion INT AUTO_INCREMENT,
    id_municipio INT NOT NULL,
    descripcion_direccion VARCHAR(255) NOT NULL,
    CONSTRAINT pk_direccion PRIMARY KEY (id_direccion),
    CONSTRAINT fk_direccion_municipio FOREIGN KEY (id_municipio)
        REFERENCES municipio (id_municipio) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS identidad(
    id_identidad INT AUTO_INCREMENT,
    dpi VARCHAR(255) NOT NULL,
    nit VARCHAR(20) NULL,
    primer_nombre VARCHAR(50) NOT NULL,
    segundo_nombre VARCHAR(50) NULL,
    tercer_nombre VARCHAR(50) NULL,
    primer_apellido VARCHAR(50) NOT NULL,
    segundo_apellido VARCHAR(50) NULL,
    fecha_nacimiento DATE NOT NULL,
    id_genero INT NOT NULL,
    telefono VARCHAR(255) NULL,
    id_direccion INT NOT NULL,
    estado VARCHAR(10) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_identidad PRIMARY KEY (id_identidad),
    CONSTRAINT uq_identidad_dpi UNIQUE (dpi),
    CONSTRAINT fk_identidad_genero FOREIGN KEY (id_genero) REFERENCES genero (id_genero) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_identidad_direccion FOREIGN KEY (id_direccion) REFERENCES direccion (id_direccion) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_identidad_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS sucursal(
    id_sucursal INT AUTO_INCREMENT,
    codigo_sucursal VARCHAR(10) NOT NULL,
    nombre VARCHAR(80) NOT NULL,
    id_municipio INT NOT NULL,
    direccion VARCHAR(200) NOT NULL,
    telefono VARCHAR(15) NULL,
    id_empleado_responsable INT NULL,
    estado VARCHAR(10) NOT NULL DEFAULT 'ACTIVA',
    fecha_apertura DATE NOT NULL,
    CONSTRAINT pk_sucursal PRIMARY KEY (id_sucursal),
    CONSTRAINT uq_sucursal_codigo UNIQUE (codigo_sucursal),
    CONSTRAINT fk_sucursal_municipio FOREIGN KEY (id_municipio) REFERENCES municipio (id_municipio) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_sucursal_estado CHECK (estado IN ('ACTIVA','INACTIVA'))
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS cliente(
    id_cliente INT AUTO_INCREMENT,
    id_identidad INT NOT NULL,
    codigo_cliente VARCHAR(15) NOT NULL,
    estado VARCHAR(10) NOT NULL DEFAULT 'ACTIVO',
    fecha_alta DATE NOT NULL,
    fecha_baja DATE NULL,
    id_usuario_registro INT NOT NULL,
    tipo_registro VARCHAR(10) NOT NULL DEFAULT 'EMPLEADO',
    CONSTRAINT pk_cliente PRIMARY KEY (id_cliente),
    CONSTRAINT uq_cliente_identidad UNIQUE (id_identidad),
    CONSTRAINT uq_cliente_codigo UNIQUE (codigo_cliente),
    CONSTRAINT fk_cliente_identidad FOREIGN KEY (id_identidad)
        REFERENCES identidad (id_identidad) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_cliente_tipo_registro CHECK (tipo_registro IN ('EMPLEADO','SISTEMA')),
    CONSTRAINT chk_cliente_estado CHECK (estado IN ('ACTIVO','INACTIVO')),
    CONSTRAINT chk_cliente_fecha_baja CHECK (fecha_baja IS NULL OR fecha_baja >= fecha_alta),
    CONSTRAINT chk_cliente_estado_baja CHECK (
        (estado = 'ACTIVO'   AND fecha_baja IS NULL) OR
        (estado = 'INACTIVO' AND fecha_baja IS NOT NULL)
    )
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS empleado(
    id_empleado INT AUTO_INCREMENT,
    id_identidad INT NOT NULL,
    codigo_empleado VARCHAR(15) NOT NULL,
    id_sucursal INT NOT NULL,
    puesto VARCHAR(50) NOT NULL,
    fecha_ingreso DATE NOT NULL,
    fecha_baja DATE NULL,
    estado VARCHAR(10) NOT NULL DEFAULT 'ACTIVO',
    id_usuario_registro INT NOT NULL,
    tipo_registro VARCHAR(10) NOT NULL DEFAULT 'EMPLEADO',
    CONSTRAINT pk_empleado PRIMARY KEY (id_empleado),
    CONSTRAINT uq_empleado_identidad UNIQUE (id_identidad),
    CONSTRAINT uq_empleado_codigo UNIQUE (codigo_empleado),
    CONSTRAINT fk_empleado_identidad FOREIGN KEY (id_identidad)
        REFERENCES identidad (id_identidad) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_empleado_sucursal FOREIGN KEY (id_sucursal)
        REFERENCES sucursal (id_sucursal) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_empleado_tipo_registro CHECK (tipo_registro IN ('EMPLEADO','SISTEMA')),
    CONSTRAINT chk_empleado_estado CHECK (estado IN ('ACTIVO','INACTIVO')),
    CONSTRAINT chk_empleado_fecha_baja CHECK (fecha_baja IS NULL OR fecha_baja >= fecha_ingreso)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS usuario(
    id_usuario INT AUTO_INCREMENT,
    tipo VARCHAR(10) NOT NULL,
    id_cliente INT NULL,
    id_empleado INT NULL,
    id_rol INT NOT NULL,
    username VARCHAR(30) NOT NULL,
    correo VARCHAR(100) NULL,
    hash_contrasena VARCHAR(255) NULL,
    hash_clave_operacion VARCHAR(255) NULL,
    requiere_cambio_contrasena TINYINT(1) NOT NULL DEFAULT 1,
    fecha_ultimo_cambio DATETIME NULL,
    intentos_fallidos INT NOT NULL DEFAULT 0,
    fecha_bloqueo DATETIME NULL,
    estado VARCHAR(10) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario_creador INT NULL,
    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uq_usuario_cliente UNIQUE (id_cliente),
    CONSTRAINT uq_usuario_empleado UNIQUE (id_empleado),
    CONSTRAINT uq_usuario_username UNIQUE (username),
    CONSTRAINT uq_usuario_correo UNIQUE (correo),
    CONSTRAINT uq_usuario_tipo UNIQUE (id_usuario, tipo),
    CONSTRAINT fk_usuario_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_usuario_empleado FOREIGN KEY (id_empleado) REFERENCES empleado (id_empleado) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol, tipo) REFERENCES rol (id_rol, tipo_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_usuario_creador FOREIGN KEY (id_usuario_creador) REFERENCES usuario (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_usuario_tipo CHECK (tipo IN ('CLIENTE','EMPLEADO','SISTEMA')),
    CONSTRAINT chk_usuario_estado CHECK (estado IN ('ACTIVO','BLOQUEADO','INACTIVO')),
    CONSTRAINT chk_usuario_intentos CHECK (intentos_fallidos >= 0),
    CONSTRAINT chk_usuario_arco CHECK (
        (tipo = 'CLIENTE'  AND id_cliente IS NOT NULL AND id_empleado IS NULL) OR
        (tipo = 'EMPLEADO' AND id_empleado IS NOT NULL AND id_cliente IS NULL) OR
        (tipo = 'SISTEMA'  AND id_cliente IS NULL     AND id_empleado IS NULL)
    ),
    CONSTRAINT chk_usuario_sistema CHECK (
        (tipo = 'SISTEMA'  AND hash_contrasena IS NULL     AND correo IS NULL) OR
        (tipo <> 'SISTEMA' AND hash_contrasena IS NOT NULL AND correo IS NOT NULL)
    )
) ENGINE = InnoDB;

ALTER TABLE sucursal
    ADD CONSTRAINT fk_sucursal_empleado_responsable FOREIGN KEY (id_empleado_responsable)
        REFERENCES empleado (id_empleado) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE cliente
    ADD CONSTRAINT fk_cliente_usuario_registro FOREIGN KEY (id_usuario_registro, tipo_registro)
        REFERENCES usuario (id_usuario, tipo) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE empleado
    ADD CONSTRAINT fk_empleado_usuario_registro FOREIGN KEY (id_usuario_registro, tipo_registro)
        REFERENCES usuario (id_usuario, tipo) ON DELETE RESTRICT ON UPDATE RESTRICT;

CREATE TABLE IF NOT EXISTS rol_permiso(
    id_rol INT NOT NULL,
    id_permiso INT NOT NULL,
    fecha_asignacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario_asigna INT NOT NULL,
    CONSTRAINT pk_rol_permiso PRIMARY KEY (id_rol, id_permiso),
    CONSTRAINT fk_rol_permiso_rol FOREIGN KEY (id_rol) REFERENCES rol (id_rol) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_rol_permiso_permiso FOREIGN KEY (id_permiso) REFERENCES permiso (id_permiso) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_rol_permiso_usuario FOREIGN KEY (id_usuario_asigna) REFERENCES usuario (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS cuenta(
    id_cuenta INT AUTO_INCREMENT,
    numero_cuenta VARCHAR(20) NOT NULL,
    id_cliente INT NOT NULL,
    id_tipo_cuenta INT NOT NULL,
    id_estado_cuenta INT NOT NULL,
    id_sucursal_apertura INT NOT NULL,
    saldo_actual DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    saldo_retenido DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    fecha_apertura DATE NOT NULL,
    fecha_cierre DATE NULL,
    id_empleado_apertura INT NOT NULL,
    alias VARCHAR(50) NULL,
    CONSTRAINT pk_cuenta PRIMARY KEY (id_cuenta),
    CONSTRAINT uq_cuenta_numero UNIQUE (numero_cuenta),
    CONSTRAINT fk_cuenta_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_cuenta_tipo_cuenta FOREIGN KEY (id_tipo_cuenta) REFERENCES tipo_cuenta (id_tipo_cuenta) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_cuenta_estado_cuenta FOREIGN KEY (id_estado_cuenta) REFERENCES estado_cuenta (id_estado_cuenta) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_cuenta_sucursal FOREIGN KEY (id_sucursal_apertura) REFERENCES sucursal (id_sucursal) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_cuenta_empleado_apertura FOREIGN KEY (id_empleado_apertura) REFERENCES empleado (id_empleado) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_cuenta_saldo_actual CHECK (saldo_actual >= 0),
    CONSTRAINT chk_cuenta_saldo_retenido CHECK (saldo_retenido >= 0 AND saldo_retenido <= saldo_actual),
    CONSTRAINT chk_cuenta_fechas CHECK (fecha_cierre IS NULL OR fecha_cierre >= fecha_apertura)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS sesion_caja(
    id_sesion_caja INT AUTO_INCREMENT,
    id_sucursal INT NOT NULL,
    id_empleado_cajero INT NOT NULL,
    fecha_apertura DATETIME NOT NULL,
    monto_apertura DECIMAL(15,2) NOT NULL,
    fecha_cierre DATETIME NULL,
    monto_cierre_declarado DECIMAL(15,2) NULL,
    monto_cierre_calculado DECIMAL(15,2) NULL,
    diferencia DECIMAL(15,2) NULL,
    cantidad_transacciones INT NOT NULL DEFAULT 0,
    estado VARCHAR(10) NOT NULL DEFAULT 'ABIERTA',
    id_empleado_supervisor INT NULL,
    fecha_arqueo DATETIME NULL,
    observaciones VARCHAR(300) NULL,
    cajero_abierto INT GENERATED ALWAYS AS (
        CASE WHEN estado = 'ABIERTA' THEN id_empleado_cajero ELSE NULL END
    ) VIRTUAL,
    CONSTRAINT pk_sesion_caja PRIMARY KEY (id_sesion_caja),
    CONSTRAINT uq_sesion_caja_cajero_abierto UNIQUE (cajero_abierto),
    CONSTRAINT fk_sesion_caja_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursal (id_sucursal) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_sesion_caja_cajero FOREIGN KEY (id_empleado_cajero) REFERENCES empleado (id_empleado) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_sesion_caja_supervisor FOREIGN KEY (id_empleado_supervisor) REFERENCES empleado (id_empleado) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_sesion_caja_estado CHECK (estado IN ('ABIERTA','CERRADA','ARQUEADA')),
    CONSTRAINT chk_sesion_caja_monto CHECK (monto_apertura >= 0),
    CONSTRAINT chk_sesion_caja_cierre CHECK (
        (estado = 'ABIERTA'  AND fecha_cierre IS NULL) OR
        (estado <> 'ABIERTA' AND fecha_cierre IS NOT NULL)
    ),
    CONSTRAINT chk_sesion_caja_cuatro_ojos CHECK (
        id_empleado_supervisor IS NULL OR id_empleado_supervisor <> id_empleado_cajero
    )
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS sesion_usuario(
    id_sesion_usuario BIGINT AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    identificador_sesion VARCHAR(64) NOT NULL,
    fecha_inicio DATETIME NOT NULL,
    fecha_ultimo_uso DATETIME NOT NULL,
    fecha_fin DATETIME NULL,
    motivo_cierre VARCHAR(30) NULL,
    direccion_ip VARCHAR(45) NULL,
    nombre_equipo VARCHAR(60) NULL,
    CONSTRAINT pk_sesion_usuario PRIMARY KEY (id_sesion_usuario),
    CONSTRAINT uq_sesion_usuario_identificador UNIQUE (identificador_sesion),
    CONSTRAINT fk_sesion_usuario_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_sesion_usuario_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio),
    CONSTRAINT chk_sesion_usuario_motivo CHECK (motivo_cierre IS NULL OR motivo_cierre IN ('LOGOUT','EXPIRACION','BLOQUEO','CIERRE_ADMIN'))
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS intento_acceso(
    id_intento BIGINT AUTO_INCREMENT,
    username_intentado VARCHAR(30) NOT NULL,
    id_usuario INT NULL,
    fecha_hora DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    exitoso TINYINT(1) NOT NULL,
    motivo_fallo VARCHAR(40) NULL,
    direccion_ip VARCHAR(45) NULL,
    CONSTRAINT pk_intento_acceso PRIMARY KEY (id_intento),
    CONSTRAINT fk_intento_acceso_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_intento_acceso_motivo CHECK (
        (exitoso = 1 AND motivo_fallo IS NULL) OR
        (exitoso = 0 AND motivo_fallo IS NOT NULL)
    )
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS transaccion(
    id_transaccion BIGINT AUTO_INCREMENT,
    numero_transaccion VARCHAR(20) NOT NULL,
    id_tipo_operacion INT NOT NULL,
    id_estado_transaccion INT NOT NULL,
    monto DECIMAL(15,2) NOT NULL,
    fecha_hora DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    canal VARCHAR(10) NOT NULL,
    id_sesion_usuario BIGINT NOT NULL,
    id_sesion_caja INT NULL,
    concepto VARCHAR(200) NULL,
    referencia VARCHAR(40) NOT NULL,
    id_transaccion_origen BIGINT NULL,
    identidad_verificada TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT pk_transaccion PRIMARY KEY (id_transaccion),
    CONSTRAINT uq_transaccion_numero UNIQUE (numero_transaccion),
    CONSTRAINT uq_transaccion_referencia UNIQUE (referencia),
    CONSTRAINT fk_transaccion_tipo_operacion FOREIGN KEY (id_tipo_operacion) REFERENCES tipo_operacion (id_tipo_operacion) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_transaccion_estado FOREIGN KEY (id_estado_transaccion) REFERENCES estado_transaccion (id_estado_transaccion) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_transaccion_sesion_usuario FOREIGN KEY (id_sesion_usuario) REFERENCES sesion_usuario (id_sesion_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_transaccion_sesion_caja FOREIGN KEY (id_sesion_caja) REFERENCES sesion_caja (id_sesion_caja) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_transaccion_origen FOREIGN KEY (id_transaccion_origen) REFERENCES transaccion (id_transaccion) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_transaccion_monto CHECK (monto > 0),
    CONSTRAINT chk_transaccion_canal CHECK (canal IN ('CAJA','VIRTUAL','APERTURA')),
    CONSTRAINT chk_transaccion_canal_caja CHECK (
        (canal IN ('VIRTUAL','APERTURA') AND id_sesion_caja IS NULL) OR
        (canal = 'CAJA' AND id_sesion_caja IS NOT NULL)
    )
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS movimiento_cuenta(
    id_movimiento BIGINT AUTO_INCREMENT,
    id_transaccion BIGINT NOT NULL,
    id_cuenta INT NOT NULL,
    naturaleza VARCHAR(6) NOT NULL,
    monto DECIMAL(15,2) NOT NULL,
    saldo_anterior DECIMAL(15,2) NOT NULL,
    saldo_posterior DECIMAL(15,2) NOT NULL,
    orden TINYINT NOT NULL,
    CONSTRAINT pk_movimiento_cuenta PRIMARY KEY (id_movimiento),
    CONSTRAINT uq_movimiento_cuenta_orden UNIQUE (id_transaccion, orden),
    CONSTRAINT fk_movimiento_cuenta_transaccion FOREIGN KEY (id_transaccion) REFERENCES transaccion (id_transaccion) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_movimiento_cuenta_cuenta FOREIGN KEY (id_cuenta) REFERENCES cuenta (id_cuenta) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_movimiento_cuenta_naturaleza CHECK (naturaleza IN ('CARGO','ABONO')),
    CONSTRAINT chk_movimiento_cuenta_orden CHECK (orden IN (1, 2)),
    CONSTRAINT chk_movimiento_cuenta_monto CHECK (monto > 0),
    CONSTRAINT chk_movimiento_cuenta_aritmetica CHECK (
        (naturaleza = 'ABONO' AND saldo_posterior = saldo_anterior + monto) OR
        (naturaleza = 'CARGO' AND saldo_posterior = saldo_anterior - monto)
    )
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS autorizacion(
    id_autorizacion INT AUTO_INCREMENT,
    id_transaccion BIGINT NOT NULL,
    motivo VARCHAR(20) NOT NULL,
    id_empleado_supervisor INT NOT NULL,
    fecha_resolucion DATETIME NOT NULL,
    resultado VARCHAR(10) NOT NULL,
    comentario VARCHAR(300) NULL,
    CONSTRAINT pk_autorizacion PRIMARY KEY (id_autorizacion),
    CONSTRAINT uq_autorizacion_transaccion UNIQUE (id_transaccion),
    CONSTRAINT fk_autorizacion_transaccion FOREIGN KEY (id_transaccion) REFERENCES transaccion (id_transaccion) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_autorizacion_supervisor FOREIGN KEY (id_empleado_supervisor) REFERENCES empleado (id_empleado) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_autorizacion_motivo CHECK (motivo IN ('SOBRE_LIMITE','REVERSO','CUENTA_BLOQUEADA')),
    CONSTRAINT chk_autorizacion_resultado CHECK (resultado IN ('APROBADA','RECHAZADA'))
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS estado_cuenta_generado(
    id_estado_cuenta_gen INT AUTO_INCREMENT,
    id_cuenta INT NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    saldo_inicial DECIMAL(15,2) NOT NULL,
    total_cargos DECIMAL(15,2) NOT NULL,
    total_abonos DECIMAL(15,2) NOT NULL,
    saldo_final DECIMAL(15,2) NOT NULL,
    cantidad_movimientos INT NOT NULL,
    fecha_generacion DATETIME NOT NULL,
    id_usuario_genera INT NOT NULL,
    CONSTRAINT pk_estado_cuenta_generado PRIMARY KEY (id_estado_cuenta_gen),
    CONSTRAINT fk_estado_cuenta_generado_cuenta FOREIGN KEY (id_cuenta) REFERENCES cuenta (id_cuenta) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_estado_cuenta_generado_usuario FOREIGN KEY (id_usuario_genera) REFERENCES usuario (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_estado_cuenta_generado_fechas CHECK (fecha_fin >= fecha_inicio),
    CONSTRAINT chk_estado_cuenta_generado_totales CHECK (total_cargos >= 0 AND total_abonos >= 0),
    CONSTRAINT chk_estado_cuenta_generado_saldo CHECK (saldo_final = saldo_inicial + total_abonos - total_cargos)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS bitacora_auditoria(
    id_bitacora BIGINT AUTO_INCREMENT,
    id_sesion_usuario BIGINT NULL,
    username VARCHAR(30) NOT NULL,
    nombre_tabla VARCHAR(50) NOT NULL,
    id_registro_afectado VARCHAR(40) NOT NULL,
    tipo_accion VARCHAR(6) NOT NULL,
    valores_anteriores JSON NULL,
    valores_nuevos JSON NULL,
    fecha_hora DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_bitacora_auditoria PRIMARY KEY (id_bitacora),
    CONSTRAINT fk_bitacora_auditoria_sesion FOREIGN KEY (id_sesion_usuario) REFERENCES sesion_usuario (id_sesion_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_bitacora_auditoria_accion CHECK (tipo_accion IN ('INSERT','UPDATE','DELETE'))
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS bitacora_consulta(
    id_bitacora_consulta BIGINT AUTO_INCREMENT,
    id_sesion_usuario BIGINT NOT NULL,
    username VARCHAR(30) NOT NULL,
    tipo_consulta VARCHAR(20) NOT NULL,
    id_cuenta INT NULL,
    id_cliente INT NULL,
    criterio VARCHAR(200) NULL,
    cantidad_registros INT NOT NULL DEFAULT 0,
    fecha_hora DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_bitacora_consulta PRIMARY KEY (id_bitacora_consulta),
    CONSTRAINT fk_bitacora_consulta_sesion FOREIGN KEY (id_sesion_usuario) REFERENCES sesion_usuario (id_sesion_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_bitacora_consulta_cuenta FOREIGN KEY (id_cuenta) REFERENCES cuenta (id_cuenta) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_bitacora_consulta_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_bitacora_consulta_tipo CHECK (
        tipo_consulta IN ('SALDO','MOVIMIENTOS','ESTADO_CUENTA','DATO_PERSONAL','BUSQUEDA_DPI')
    )
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS alerta(
    id_alerta BIGINT AUTO_INCREMENT,
    id_tipo_alerta INT NOT NULL,
    id_transaccion BIGINT NOT NULL,
    id_cuenta INT NOT NULL,
    fecha_deteccion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    detalle VARCHAR(300) NOT NULL,
    estado VARCHAR(12) NOT NULL DEFAULT 'PENDIENTE',
    id_empleado_revisor INT NULL,
    fecha_revision DATETIME NULL,
    resolucion VARCHAR(300) NULL,
    CONSTRAINT pk_alerta PRIMARY KEY (id_alerta),
    CONSTRAINT fk_alerta_tipo_alerta FOREIGN KEY (id_tipo_alerta) REFERENCES tipo_alerta (id_tipo_alerta) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_alerta_transaccion FOREIGN KEY (id_transaccion) REFERENCES transaccion (id_transaccion) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_alerta_cuenta FOREIGN KEY (id_cuenta) REFERENCES cuenta (id_cuenta) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_alerta_revisor FOREIGN KEY (id_empleado_revisor) REFERENCES empleado (id_empleado) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_alerta_estado CHECK (estado IN ('PENDIENTE','EN_REVISION','RESUELTA','DESCARTADA')),
    CONSTRAINT chk_alerta_resolucion CHECK (
        estado IN ('PENDIENTE','EN_REVISION') OR
        (estado IN ('RESUELTA','DESCARTADA') AND id_empleado_revisor IS NOT NULL AND fecha_revision IS NOT NULL)
    )
) ENGINE = InnoDB;
