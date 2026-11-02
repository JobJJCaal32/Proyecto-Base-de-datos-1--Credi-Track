package gt.umg.creditrack.caja;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gt.umg.creditrack.catalogo.EstadoCuentaRepository;
import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.config.CrediTrackProperties;
import gt.umg.creditrack.cuenta.Cuenta;
import gt.umg.creditrack.cuenta.CuentaRepository;
import gt.umg.creditrack.cuenta.SaldoService;
import gt.umg.creditrack.seguridad.UsuarioActual;
import gt.umg.creditrack.sucursal.EmpleadoRepository;
import gt.umg.creditrack.sucursal.SucursalRepository;
import gt.umg.creditrack.supervision.AlertaService;
import gt.umg.creditrack.transaccion.Canal;
import gt.umg.creditrack.transaccion.Naturaleza;
import gt.umg.creditrack.transaccion.RegistroTransacciones;
import gt.umg.creditrack.transaccion.Transaccion;
import gt.umg.creditrack.transaccion.TransaccionRepository;

@Service
public class CajaService {

    public record ResultadoOperacion(String numeroTransaccion, boolean pendiente) {
    }

    private static final String PATRON_REFERENCIA = "^CAJ-[0-9a-f]{16}$";
    private static final String SQL_RESUMEN = """
            SELECT op.nombre AS operacion, et.nombre AS estado, COUNT(*) AS cantidad, COALESCE(SUM(t.monto), 0) AS total
              FROM transaccion t
              JOIN tipo_operacion op ON op.id_tipo_operacion = t.id_tipo_operacion
              JOIN estado_transaccion et ON et.id_estado_transaccion = t.id_estado_transaccion
             WHERE t.id_sesion_caja = ?
             GROUP BY op.nombre, et.nombre
            """;

    private final SesionCajaRepository sesiones;
    private final EmpleadoRepository empleados;
    private final SucursalRepository sucursales;
    private final CuentaRepository cuentas;
    private final EstadoCuentaRepository estadosCuenta;
    private final TransaccionRepository transacciones;
    private final RegistroTransacciones registro;
    private final SaldoService saldos;
    private final AlertaService alertas;
    private final JdbcTemplate jdbc;
    private final CrediTrackProperties propiedades;

    public CajaService(SesionCajaRepository sesiones, EmpleadoRepository empleados, SucursalRepository sucursales,
                       CuentaRepository cuentas, EstadoCuentaRepository estadosCuenta, TransaccionRepository transacciones,
                       RegistroTransacciones registro, SaldoService saldos, AlertaService alertas, JdbcTemplate jdbc,
                       CrediTrackProperties propiedades) {
        this.sesiones = sesiones;
        this.empleados = empleados;
        this.sucursales = sucursales;
        this.cuentas = cuentas;
        this.estadosCuenta = estadosCuenta;
        this.transacciones = transacciones;
        this.registro = registro;
        this.saldos = saldos;
        this.alertas = alertas;
        this.jdbc = jdbc;
        this.propiedades = propiedades;
    }

    public static String nuevaReferencia() {
        return "CAJ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public BigDecimal limiteRetiro() {
        return propiedades.caja().limiteRetiroSinAutorizacion();
    }

    @Transactional(readOnly = true)
    public Optional<SesionCaja> abierta(UsuarioActual cajero) {
        return sesiones.findByIdEmpleadoCajeroAndEstado(cajero.idEmpleado(), EstadoCaja.ABIERTA);
    }

    @Transactional(readOnly = true)
    public Optional<ResumenCaja> resumenAbierta(UsuarioActual cajero) {
        return abierta(cajero).map(this::resumen);
    }

    @Transactional(readOnly = true)
    public ResumenCaja resumen(SesionCaja sesion) {
        BigDecimal depositos = BigDecimal.ZERO;
        BigDecimal retiros = BigDecimal.ZERO;
        long cantidadDepositos = 0;
        long cantidadRetiros = 0;
        long transferencias = 0;
        long pendientes = 0;
        for (Map<String, Object> fila : jdbc.queryForList(SQL_RESUMEN, sesion.getIdSesionCaja())) {
            String operacion = (String) fila.get("operacion");
            String estado = (String) fila.get("estado");
            long cantidad = ((Number) fila.get("cantidad")).longValue();
            BigDecimal total = (BigDecimal) fila.get("total");
            if (RegistroTransacciones.PENDIENTE.equals(estado)) {
                pendientes += cantidad;
            }
            if (!RegistroTransacciones.APLICADA.equals(estado) && !RegistroTransacciones.REVERSADA.equals(estado)) {
                continue;
            }
            switch (operacion) {
                case "DEPOSITO" -> {
                    depositos = depositos.add(total);
                    cantidadDepositos += cantidad;
                }
                case "RETIRO" -> {
                    retiros = retiros.add(total);
                    cantidadRetiros += cantidad;
                }
                default -> transferencias += cantidad;
            }
        }
        String sucursal = sucursales.findById(sesion.getIdSucursal()).map(s -> s.getNombre()).orElse("");
        return new ResumenCaja(sesion, sucursal, depositos, cantidadDepositos, retiros, cantidadRetiros, transferencias,
                pendientes, sesion.getMontoApertura().add(depositos).subtract(retiros));
    }

    @Transactional
    public void abrir(UsuarioActual cajero, BigDecimal montoApertura) {
        if (montoApertura == null || montoApertura.signum() < 0 || montoApertura.scale() > 2) {
            throw new ReglaNegocioException("Escriba el efectivo inicial de la caja, con máximo 2 decimales.");
        }
        if (abierta(cajero).isPresent()) {
            throw new ReglaNegocioException("Ya tiene una caja abierta.");
        }
        SesionCaja sesion = new SesionCaja();
        sesion.setIdSucursal(empleados.findById(cajero.idEmpleado()).orElseThrow().getIdSucursal());
        sesion.setIdEmpleadoCajero(cajero.idEmpleado());
        sesion.setFechaApertura(ahora());
        sesion.setMontoApertura(montoApertura);
        sesion.setEstado(EstadoCaja.ABIERTA);
        sesiones.save(sesion);
    }

    @Transactional
    public ResumenCaja cerrar(UsuarioActual cajero, BigDecimal declarado, String observaciones) {
        SesionCaja sesion = cajaRequerida(cajero);
        if (declarado == null || declarado.signum() < 0 || declarado.scale() > 2) {
            throw new ReglaNegocioException("Escriba el efectivo contado en la caja, con máximo 2 decimales.");
        }
        ResumenCaja resumen = resumen(sesion);
        if (resumen.pendientes() > 0) {
            throw new ReglaNegocioException("Hay operaciones pendientes de autorización; espere a que el supervisor las resuelva.");
        }
        sesion.setMontoCierreDeclarado(declarado);
        sesion.setMontoCierreCalculado(resumen.efectivoEsperado());
        sesion.setDiferencia(declarado.subtract(resumen.efectivoEsperado()));
        sesion.setObservaciones(limpiar(observaciones, 300));
        sesion.setFechaCierre(ahora());
        sesion.setEstado(EstadoCaja.CERRADA);
        return resumen(sesion);
    }

    @Transactional
    public ResultadoOperacion depositar(UsuarioActual cajero, String numeroCuenta, BigDecimal monto, String concepto, String token) {
        SesionCaja sesion = cajaRequerida(cajero);
        String referencia = referenciaCompleta(token, numeroCuenta);
        Transaccion previa = previa(referencia);
        if (previa != null) {
            return new ResultadoOperacion(previa.getNumeroTransaccion(), false);
        }
        validarMonto(monto);
        Cuenta cuenta = cuentaOperable(numeroCuenta);
        Transaccion t = registro.nueva("DEPOSITO", RegistroTransacciones.APLICADA, monto, Canal.CAJA, cajero.idSesionUsuario(),
                sesion.getIdSesionCaja(), textoOr(concepto, "Depósito en ventanilla"), referencia, false, null);
        saldos.registrarMovimiento(t.getIdTransaccion(), cuenta.getIdCuenta(), Naturaleza.ABONO, monto, 1);
        alertas.evaluarMonto(t.getIdTransaccion(), cuenta.getIdCuenta(), cuenta.getNumeroCuenta(), monto, "Depósito en efectivo");
        contar(sesion);
        return new ResultadoOperacion(t.getNumeroTransaccion(), false);
    }

    @Transactional
    public ResultadoOperacion retirar(UsuarioActual cajero, String numeroCuenta, BigDecimal monto, boolean identidadVerificada,
                                      String concepto, String token) {
        SesionCaja sesion = cajaRequerida(cajero);
        String referencia = referenciaCompleta(token, numeroCuenta);
        Transaccion previa = previa(referencia);
        if (previa != null) {
            return new ResultadoOperacion(previa.getNumeroTransaccion(),
                    RegistroTransacciones.PENDIENTE.equals(registro.nombreEstado(previa.getIdEstadoTransaccion())));
        }
        validarMonto(monto);
        if (!identidadVerificada) {
            throw new ReglaNegocioException("Verifique el DPI del titular antes de entregar efectivo.");
        }
        Cuenta cuenta = cuentaOperable(numeroCuenta);
        if (resumen(sesion).efectivoEsperado().compareTo(monto) < 0) {
            throw new ReglaNegocioException("La caja no tiene suficiente efectivo para este retiro.");
        }
        if (cuenta.getSaldoActual().subtract(cuenta.getSaldoRetenido()).compareTo(monto) < 0) {
            throw new ReglaNegocioException("La cuenta " + cuenta.getNumeroCuenta() + " no tiene saldo disponible suficiente.");
        }
        boolean requiereAutorizacion = monto.compareTo(limiteRetiro()) > 0;
        Transaccion t = registro.nueva("RETIRO",
                requiereAutorizacion ? RegistroTransacciones.PENDIENTE : RegistroTransacciones.APLICADA,
                monto, Canal.CAJA, cajero.idSesionUsuario(), sesion.getIdSesionCaja(), textoOr(concepto, "Retiro en ventanilla"),
                referencia, true, null);
        if (requiereAutorizacion) {
            return new ResultadoOperacion(t.getNumeroTransaccion(), true);
        }
        saldos.registrarMovimiento(t.getIdTransaccion(), cuenta.getIdCuenta(), Naturaleza.CARGO, monto, 1);
        alertas.evaluarCargo(t.getIdTransaccion(), cuenta.getIdCuenta(), cuenta.getNumeroCuenta(), monto, "Retiro en efectivo");
        contar(sesion);
        return new ResultadoOperacion(t.getNumeroTransaccion(), false);
    }

    @Transactional
    public ResultadoOperacion transferir(UsuarioActual cajero, String numeroOrigen, String numeroDestino, BigDecimal monto,
                                         boolean identidadVerificada, String concepto, String token) {
        SesionCaja sesion = cajaRequerida(cajero);
        String referencia = referenciaCompleta(token, numeroOrigen);
        Transaccion previa = previa(referencia);
        if (previa != null) {
            return new ResultadoOperacion(previa.getNumeroTransaccion(), false);
        }
        validarMonto(monto);
        if (!identidadVerificada) {
            throw new ReglaNegocioException("Verifique el DPI del titular de la cuenta origen.");
        }
        Cuenta origen = cuentaOperable(numeroOrigen);
        Cuenta destino = cuentaOperable(numeroDestino);
        if (origen.getIdCuenta().equals(destino.getIdCuenta())) {
            throw new ReglaNegocioException("La cuenta destino debe ser distinta de la cuenta origen.");
        }
        saldos.bloquearEnOrden(List.of(origen.getIdCuenta(), destino.getIdCuenta()));
        Transaccion t = registro.nueva("TRANSFERENCIA", RegistroTransacciones.APLICADA, monto, Canal.CAJA, cajero.idSesionUsuario(),
                sesion.getIdSesionCaja(), textoOr(concepto, "Transferencia en ventanilla"), referencia, true, null);
        saldos.registrarMovimiento(t.getIdTransaccion(), origen.getIdCuenta(), Naturaleza.CARGO, monto, 1);
        saldos.registrarMovimiento(t.getIdTransaccion(), destino.getIdCuenta(), Naturaleza.ABONO, monto, 2);
        alertas.evaluarCargo(t.getIdTransaccion(), origen.getIdCuenta(), origen.getNumeroCuenta(), monto, "Transferencia en ventanilla");
        contar(sesion);
        return new ResultadoOperacion(t.getNumeroTransaccion(), false);
    }

    @Transactional(readOnly = true)
    public Optional<Transaccion> transaccionDeCajero(UsuarioActual cajero, String numero) {
        return transacciones.findByNumeroTransaccion(numero)
                .filter(t -> t.getIdSesionCaja() != null)
                .filter(t -> sesiones.findById(t.getIdSesionCaja())
                        .map(s -> s.getIdEmpleadoCajero().equals(cajero.idEmpleado())).orElse(false));
    }

    private SesionCaja cajaRequerida(UsuarioActual cajero) {
        return abierta(cajero).orElseThrow(() -> new ReglaNegocioException("Primero abra su caja."));
    }

    public static String cuentaDeReferencia(String referencia) {
        int separador = referencia == null ? -1 : referencia.lastIndexOf('-');
        return separador < 0 ? "" : referencia.substring(separador + 1);
    }

    private static String referenciaCompleta(String token, String numeroCuenta) {
        if (token == null || !token.matches(PATRON_REFERENCIA)) {
            throw new ReglaNegocioException("La operación no es válida. Vuelva a abrir el formulario.");
        }
        return token + "-" + (numeroCuenta == null ? "" : numeroCuenta.replaceAll("[\\s-]", ""));
    }

    private Transaccion previa(String referencia) {
        return transacciones.findByReferencia(referencia).orElse(null);
    }

    private Cuenta cuentaOperable(String numero) {
        String limpio = numero == null ? "" : numero.replaceAll("[\\s-]", "");
        Cuenta cuenta = cuentas.findByNumeroCuenta(limpio)
                .orElseThrow(() -> new ReglaNegocioException("No existe la cuenta " + limpio + "."));
        boolean opera = estadosCuenta.findById(cuenta.getIdEstadoCuenta()).map(e -> e.getPermiteOperar()).orElse(false);
        if (!opera) {
            throw new ReglaNegocioException("La cuenta " + limpio + " no permite operar en su estado actual.");
        }
        return cuenta;
    }

    private void contar(SesionCaja sesion) {
        sesion.setCantidadTransacciones(sesion.getCantidadTransacciones() + 1);
    }

    private static void validarMonto(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0 || monto.scale() > 2) {
            throw new ReglaNegocioException("Escriba un monto mayor que cero, con máximo 2 decimales.");
        }
    }

    private static String textoOr(String texto, String predeterminado) {
        String limpio = limpiar(texto, 200);
        return limpio == null ? predeterminado : limpio;
    }

    private static String limpiar(String texto, int maximo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.trim().replaceAll("\\s+", " ");
        return limpio.length() > maximo ? limpio.substring(0, maximo) : limpio;
    }

    private static LocalDateTime ahora() {
        return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }
}
