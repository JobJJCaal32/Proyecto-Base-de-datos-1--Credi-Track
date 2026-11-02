package gt.umg.creditrack.cuenta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gt.umg.creditrack.catalogo.EstadoCuentaRepository;
import gt.umg.creditrack.catalogo.EstadoTransaccionRepository;
import gt.umg.creditrack.catalogo.TipoCuenta;
import gt.umg.creditrack.catalogo.TipoCuentaRepository;
import gt.umg.creditrack.catalogo.TipoOperacionRepository;
import gt.umg.creditrack.cliente.Cliente;
import gt.umg.creditrack.cliente.ClienteRepository;
import gt.umg.creditrack.comun.bd.GeneradorCodigos;
import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.comun.modelo.EstadoActivo;
import gt.umg.creditrack.comun.web.Formato;
import gt.umg.creditrack.persona.Identidad;
import gt.umg.creditrack.persona.IdentidadRepository;
import gt.umg.creditrack.persona.NombreCompleto;
import gt.umg.creditrack.seguridad.UsuarioActual;
import gt.umg.creditrack.sucursal.Empleado;
import gt.umg.creditrack.sucursal.EmpleadoRepository;
import gt.umg.creditrack.transaccion.Canal;
import gt.umg.creditrack.transaccion.Naturaleza;
import gt.umg.creditrack.transaccion.Transaccion;
import gt.umg.creditrack.transaccion.TransaccionRepository;

@Service
public class AperturaCuentaService {

    public static final String ESTADO_INICIAL = "ACTIVA";
    public static final String OPERACION_DEPOSITO = "DEPOSITO";
    public static final String TRANSACCION_APLICADA = "APLICADA";

    private final ClienteRepository clientes;
    private final IdentidadRepository identidades;
    private final TipoCuentaRepository tiposCuenta;
    private final EstadoCuentaRepository estadosCuenta;
    private final TipoOperacionRepository tiposOperacion;
    private final EstadoTransaccionRepository estadosTransaccion;
    private final EmpleadoRepository empleados;
    private final CuentaRepository cuentas;
    private final TransaccionRepository transacciones;
    private final GeneradorCodigos generador;
    private final SaldoService saldos;
    private final Formato formato;

    public AperturaCuentaService(ClienteRepository clientes, IdentidadRepository identidades, TipoCuentaRepository tiposCuenta,
                                 EstadoCuentaRepository estadosCuenta, TipoOperacionRepository tiposOperacion,
                                 EstadoTransaccionRepository estadosTransaccion, EmpleadoRepository empleados,
                                 CuentaRepository cuentas, TransaccionRepository transacciones,
                                 GeneradorCodigos generador, SaldoService saldos, Formato formato) {
        this.clientes = clientes;
        this.identidades = identidades;
        this.tiposCuenta = tiposCuenta;
        this.estadosCuenta = estadosCuenta;
        this.tiposOperacion = tiposOperacion;
        this.estadosTransaccion = estadosTransaccion;
        this.empleados = empleados;
        this.cuentas = cuentas;
        this.transacciones = transacciones;
        this.generador = generador;
        this.saldos = saldos;
        this.formato = formato;
    }

    @Transactional(readOnly = true)
    public TipoCuenta validarApertura(Integer idTipoCuenta, BigDecimal monto) {
        TipoCuenta tipo = tiposCuenta.findById(idTipoCuenta == null ? -1 : idTipoCuenta)
                .filter(TipoCuenta::getActivo)
                .orElseThrow(() -> new ReglaNegocioException("Seleccione un tipo de cuenta válido."));
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaNegocioException("El depósito de apertura debe ser mayor que cero.");
        }
        if (monto.compareTo(tipo.getMontoAperturaMinimo()) < 0) {
            throw new ReglaNegocioException("Una cuenta " + tipo.getNombre().toLowerCase(Locale.ROOT)
                    + " se abre con un depósito de al menos " + formato.moneda(tipo.getMontoAperturaMinimo()) + ".");
        }
        return tipo;
    }

    @Transactional
    public ResultadoApertura abrirParaCodigo(String codigoCliente, Integer idTipoCuenta, BigDecimal monto, UsuarioActual empleado) {
        String codigo = codigoCliente == null ? "" : codigoCliente.trim().toUpperCase(Locale.ROOT);
        Cliente cliente = clientes.findByCodigoCliente(codigo)
                .orElseThrow(() -> new ReglaNegocioException("No existe un cliente con el código " + codigo + "."));
        return abrir(cliente.getIdCliente(), idTipoCuenta, monto, empleado);
    }

    @Transactional
    public ResultadoApertura abrir(Integer idCliente, Integer idTipoCuenta, BigDecimal monto, UsuarioActual empleado) {
        TipoCuenta tipo = validarApertura(idTipoCuenta, monto);
        Cliente cliente = clientes.findById(idCliente)
                .orElseThrow(() -> new ReglaNegocioException("El cliente no existe."));
        if (cliente.getEstado() != EstadoActivo.ACTIVO) {
            throw new ReglaNegocioException("El cliente " + cliente.getCodigoCliente() + " está dado de baja; no se le pueden abrir cuentas.");
        }
        if (empleado.idEmpleado() == null) {
            throw new ReglaNegocioException("Solo un empleado puede abrir cuentas.");
        }
        Empleado responsable = empleados.findById(empleado.idEmpleado())
                .orElseThrow(() -> new ReglaNegocioException("No se encontró el empleado que abre la cuenta."));

        LocalDateTime ahora = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta(generador.siguienteNumeroCuenta());
        cuenta.setIdCliente(cliente.getIdCliente());
        cuenta.setIdTipoCuenta(tipo.getIdTipoCuenta());
        cuenta.setIdEstadoCuenta(estadosCuenta.findByNombre(ESTADO_INICIAL).orElseThrow().getIdEstadoCuenta());
        cuenta.setIdSucursalApertura(responsable.getIdSucursal());
        cuenta.setFechaApertura(LocalDate.now(ZoneOffset.UTC));
        cuenta.setIdEmpleadoApertura(responsable.getIdEmpleado());
        cuentas.save(cuenta);

        Transaccion deposito = new Transaccion();
        deposito.setNumeroTransaccion(generador.siguienteNumeroTransaccion());
        deposito.setIdTipoOperacion(tiposOperacion.findByNombre(OPERACION_DEPOSITO).orElseThrow().getIdTipoOperacion());
        deposito.setIdEstadoTransaccion(estadosTransaccion.findByNombre(TRANSACCION_APLICADA).orElseThrow().getIdEstadoTransaccion());
        deposito.setMonto(monto);
        deposito.setFechaHora(ahora);
        deposito.setCanal(Canal.APERTURA);
        deposito.setIdSesionUsuario(empleado.idSesionUsuario());
        deposito.setConcepto("Depósito de apertura");
        deposito.setReferencia("APERTURA-" + cuenta.getNumeroCuenta());
        deposito.setIdentidadVerificada(Boolean.TRUE);
        transacciones.save(deposito);

        saldos.registrarMovimiento(deposito.getIdTransaccion(), cuenta.getIdCuenta(), Naturaleza.ABONO, monto, 1);

        Identidad titular = identidades.findById(cliente.getIdIdentidad()).orElseThrow();
        return new ResultadoApertura(cuenta.getIdCuenta(), cuenta.getNumeroCuenta(), tipo.getNombre(), monto,
                deposito.getNumeroTransaccion(), ahora, cliente.getCodigoCliente(), NombreCompleto.de(titular));
    }
}
