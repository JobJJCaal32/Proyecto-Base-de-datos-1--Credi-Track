package gt.umg.creditrack.transaccion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gt.umg.creditrack.catalogo.EstadoCuenta;
import gt.umg.creditrack.catalogo.EstadoCuentaRepository;
import gt.umg.creditrack.catalogo.EstadoTransaccionRepository;
import gt.umg.creditrack.catalogo.TipoOperacionRepository;
import gt.umg.creditrack.cliente.Cliente;
import gt.umg.creditrack.cliente.ClienteRepository;
import gt.umg.creditrack.comun.bd.GeneradorCodigos;
import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.cuenta.Cuenta;
import gt.umg.creditrack.cuenta.CuentaRepository;
import gt.umg.creditrack.cuenta.SaldoService;
import gt.umg.creditrack.persona.Identidad;
import gt.umg.creditrack.persona.IdentidadRepository;
import gt.umg.creditrack.seguridad.UsuarioActual;
import gt.umg.creditrack.supervision.AlertaService;
import gt.umg.creditrack.usuario.Usuario;
import gt.umg.creditrack.usuario.UsuarioRepository;

@Service
public class TransferenciaService {

    public static final String OPERACION = "TRANSFERENCIA";
    private static final String PATRON_REFERENCIA = "^VIR-[0-9a-f]{32}$";

    public record Vista(Cuenta origen, Cuenta destino, String titularDestino, BigDecimal monto, String concepto,
                        String referencia, boolean requiereClave) {
    }

    private final CuentaRepository cuentas;
    private final EstadoCuentaRepository estadosCuenta;
    private final ClienteRepository clientes;
    private final IdentidadRepository identidades;
    private final UsuarioRepository usuarios;
    private final TipoOperacionRepository tiposOperacion;
    private final EstadoTransaccionRepository estadosTransaccion;
    private final TransaccionRepository transacciones;
    private final GeneradorCodigos generador;
    private final SaldoService saldos;
    private final AlertaService alertas;
    private final PasswordEncoder codificador;

    public TransferenciaService(CuentaRepository cuentas, EstadoCuentaRepository estadosCuenta, ClienteRepository clientes,
                                IdentidadRepository identidades, UsuarioRepository usuarios,
                                TipoOperacionRepository tiposOperacion, EstadoTransaccionRepository estadosTransaccion,
                                TransaccionRepository transacciones, GeneradorCodigos generador, SaldoService saldos,
                                AlertaService alertas, PasswordEncoder codificador) {
        this.cuentas = cuentas;
        this.estadosCuenta = estadosCuenta;
        this.clientes = clientes;
        this.identidades = identidades;
        this.usuarios = usuarios;
        this.tiposOperacion = tiposOperacion;
        this.estadosTransaccion = estadosTransaccion;
        this.transacciones = transacciones;
        this.generador = generador;
        this.saldos = saldos;
        this.alertas = alertas;
        this.codificador = codificador;
    }

    @Transactional(readOnly = true)
    public List<Cuenta> cuentasOrigen(UsuarioActual cliente) {
        return cuentas.findByIdClienteOrderByFechaAperturaAscNumeroCuentaAsc(cliente.idCliente()).stream()
                .filter(c -> estadosCuenta.findById(c.getIdEstadoCuenta()).map(EstadoCuenta::getPermiteOperar).orElse(false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Cuenta> cuentasDelCliente(UsuarioActual cliente) {
        return cuentas.findByIdClienteOrderByFechaAperturaAscNumeroCuentaAsc(cliente.idCliente());
    }

    @Transactional(readOnly = true)
    public boolean tieneClaveOperacion(UsuarioActual cliente) {
        return usuarios.findById(cliente.idUsuario()).map(u -> u.getHashClaveOperacion() != null).orElse(false);
    }

    @Transactional(readOnly = true)
    public Vista preparar(UsuarioActual cliente, Integer idCuentaOrigen, String numeroDestino, BigDecimal monto, String concepto) {
        Cuenta origen = cuentaPropia(cliente, idCuentaOrigen);
        Cuenta destino = cuentaDestino(numeroDestino);
        validar(origen, destino, monto, concepto);
        if (origen.getSaldoActual().subtract(origen.getSaldoRetenido()).compareTo(monto) < 0) {
            throw new ReglaNegocioException("La cuenta " + origen.getNumeroCuenta() + " no tiene saldo disponible suficiente.");
        }
        return new Vista(origen, destino, titularResumido(destino), monto, limpiar(concepto),
                "VIR-" + UUID.randomUUID().toString().replace("-", ""), tieneClaveOperacion(cliente));
    }

    @Transactional
    public String transferir(UsuarioActual cliente, Integer idCuentaOrigen, String numeroDestino, BigDecimal monto,
                             String concepto, String referencia, String claveOperacion) {
        if (referencia == null || !referencia.matches(PATRON_REFERENCIA)) {
            throw new ReglaNegocioException("La solicitud de transferencia no es válida. Empiece de nuevo.");
        }
        Transaccion existente = transacciones.findByReferencia(referencia).orElse(null);
        if (existente != null) {
            return existente.getNumeroTransaccion();
        }
        Usuario usuario = usuarios.findById(cliente.idUsuario()).orElseThrow();
        if (usuario.getHashClaveOperacion() != null
                && (claveOperacion == null || !codificador.matches(claveOperacion, usuario.getHashClaveOperacion()))) {
            throw new ReglaNegocioException("La clave de operación no es correcta.");
        }
        Cuenta origen = cuentaPropia(cliente, idCuentaOrigen);
        Cuenta destino = cuentaDestino(numeroDestino);
        validar(origen, destino, monto, concepto);

        saldos.bloquearEnOrden(List.of(origen.getIdCuenta(), destino.getIdCuenta()));

        Transaccion transferencia = new Transaccion();
        transferencia.setNumeroTransaccion(generador.siguienteNumeroTransaccion());
        transferencia.setIdTipoOperacion(tiposOperacion.findByNombre(OPERACION).orElseThrow().getIdTipoOperacion());
        transferencia.setIdEstadoTransaccion(estadosTransaccion.findByNombre("APLICADA").orElseThrow().getIdEstadoTransaccion());
        transferencia.setMonto(monto);
        transferencia.setFechaHora(LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
        transferencia.setCanal(Canal.VIRTUAL);
        transferencia.setIdSesionUsuario(cliente.idSesionUsuario());
        transferencia.setConcepto(limpiar(concepto) == null ? "Transferencia" : limpiar(concepto));
        transferencia.setReferencia(referencia);
        transferencia.setIdentidadVerificada(Boolean.FALSE);
        transacciones.save(transferencia);

        saldos.registrarMovimiento(transferencia.getIdTransaccion(), origen.getIdCuenta(), Naturaleza.CARGO, monto, 1);
        saldos.registrarMovimiento(transferencia.getIdTransaccion(), destino.getIdCuenta(), Naturaleza.ABONO, monto, 2);
        alertas.evaluarCargo(transferencia.getIdTransaccion(), origen.getIdCuenta(), origen.getNumeroCuenta(), monto, "Transferencia");
        return transferencia.getNumeroTransaccion();
    }

    private Cuenta cuentaPropia(UsuarioActual cliente, Integer idCuenta) {
        Cuenta cuenta = cuentas.findById(idCuenta == null ? -1 : idCuenta)
                .filter(c -> c.getIdCliente().equals(cliente.idCliente()))
                .orElseThrow(() -> new ReglaNegocioException("Seleccione una de sus cuentas como origen."));
        exigirQuePermitaOperar(cuenta);
        return cuenta;
    }

    private Cuenta cuentaDestino(String numero) {
        String limpio = numero == null ? "" : numero.replaceAll("[\\s-]", "");
        Cuenta cuenta = cuentas.findByNumeroCuenta(limpio)
                .orElseThrow(() -> new ReglaNegocioException("No existe la cuenta destino " + limpio + "."));
        exigirQuePermitaOperar(cuenta);
        return cuenta;
    }

    private void exigirQuePermitaOperar(Cuenta cuenta) {
        EstadoCuenta estado = estadosCuenta.findById(cuenta.getIdEstadoCuenta()).orElseThrow();
        if (!estado.getPermiteOperar()) {
            throw new ReglaNegocioException("La cuenta " + cuenta.getNumeroCuenta() + " está "
                    + estado.getNombre().toLowerCase(Locale.ROOT) + " y no permite operar.");
        }
    }

    private static void validar(Cuenta origen, Cuenta destino, BigDecimal monto, String concepto) {
        if (origen.getIdCuenta().equals(destino.getIdCuenta())) {
            throw new ReglaNegocioException("La cuenta destino debe ser distinta de la cuenta origen.");
        }
        if (monto == null || monto.signum() <= 0 || monto.scale() > 2) {
            throw new ReglaNegocioException("Escriba un monto mayor que cero, con máximo 2 decimales.");
        }
        if (concepto != null && concepto.length() > 200) {
            throw new ReglaNegocioException("El concepto admite máximo 200 caracteres.");
        }
    }

    private String titularResumido(Cuenta cuenta) {
        Cliente cliente = clientes.findById(cuenta.getIdCliente()).orElseThrow();
        Identidad identidad = identidades.findById(cliente.getIdIdentidad()).orElseThrow();
        return identidad.getPrimerNombre() + " " + identidad.getPrimerApellido().charAt(0) + ".";
    }

    private static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim().replaceAll("\\s+", " ");
        return limpio.isEmpty() ? null : limpio;
    }
}
