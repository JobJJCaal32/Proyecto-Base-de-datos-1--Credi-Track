package gt.umg.creditrack.cuenta;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gt.umg.creditrack.catalogo.EstadoCuenta;
import gt.umg.creditrack.catalogo.EstadoCuentaRepository;
import gt.umg.creditrack.catalogo.TipoCuentaRepository;
import gt.umg.creditrack.cliente.Cliente;
import gt.umg.creditrack.cliente.ClienteRepository;
import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.persona.IdentidadRepository;
import gt.umg.creditrack.persona.NombreCompleto;

@Service
public class GestionCuentaService {

    public record FichaCuenta(Cuenta cuenta, String tipo, EstadoCuenta estado, String codigoCliente, String titular) {
    }

    private final CuentaRepository cuentas;
    private final EstadoCuentaRepository estados;
    private final TipoCuentaRepository tipos;
    private final ClienteRepository clientes;
    private final IdentidadRepository identidades;

    public GestionCuentaService(CuentaRepository cuentas, EstadoCuentaRepository estados, TipoCuentaRepository tipos,
                                ClienteRepository clientes, IdentidadRepository identidades) {
        this.cuentas = cuentas;
        this.estados = estados;
        this.tipos = tipos;
        this.clientes = clientes;
        this.identidades = identidades;
    }

    @Transactional(readOnly = true)
    public Optional<FichaCuenta> buscar(String numeroCuenta) {
        String numero = numeroCuenta == null ? "" : numeroCuenta.replaceAll("[\\s-]", "");
        return cuentas.findByNumeroCuenta(numero).map(this::ficha);
    }

    @Transactional(readOnly = true)
    public List<EstadoCuenta> estados() {
        return estados.findAll();
    }

    @Transactional
    public void cambiarEstado(String numeroCuenta, Integer idEstadoNuevo) {
        Cuenta cuenta = cuentas.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new ReglaNegocioException("La cuenta " + numeroCuenta + " no existe."));
        EstadoCuenta nuevo = estados.findById(idEstadoNuevo == null ? -1 : idEstadoNuevo)
                .orElseThrow(() -> new ReglaNegocioException("Seleccione un estado válido."));
        if (nuevo.getIdEstadoCuenta().equals(cuenta.getIdEstadoCuenta())) {
            throw new ReglaNegocioException("La cuenta ya está " + nuevo.getNombre() + ".");
        }
        cuenta.setIdEstadoCuenta(nuevo.getIdEstadoCuenta());
        cuentas.flush();
    }

    private FichaCuenta ficha(Cuenta cuenta) {
        Cliente cliente = clientes.findById(cuenta.getIdCliente()).orElseThrow();
        return new FichaCuenta(cuenta,
                tipos.findById(cuenta.getIdTipoCuenta()).map(t -> t.getNombre()).orElse(""),
                estados.findById(cuenta.getIdEstadoCuenta()).orElseThrow(),
                cliente.getCodigoCliente(),
                identidades.findById(cliente.getIdIdentidad()).map(NombreCompleto::de).orElse(""));
    }
}
