package gt.umg.creditrack.cliente;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gt.umg.creditrack.auditoria.BitacoraConsulta;
import gt.umg.creditrack.auditoria.BitacoraConsultaRepository;
import gt.umg.creditrack.auditoria.TipoConsulta;
import gt.umg.creditrack.catalogo.Departamento;
import gt.umg.creditrack.catalogo.DepartamentoRepository;
import gt.umg.creditrack.catalogo.GeneroRepository;
import gt.umg.creditrack.catalogo.Municipio;
import gt.umg.creditrack.catalogo.MunicipioRepository;
import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.comun.web.Formato;
import gt.umg.creditrack.cuenta.ConsultaCuentasService;
import gt.umg.creditrack.persona.Direccion;
import gt.umg.creditrack.persona.DireccionRepository;
import gt.umg.creditrack.persona.Identidad;
import gt.umg.creditrack.persona.IdentidadRepository;
import gt.umg.creditrack.persona.NombreCompleto;
import gt.umg.creditrack.seguridad.UsuarioActual;
import gt.umg.creditrack.usuario.UsuarioRepository;

@Service
public class ConsultaClienteService {

    private static final String PATRON_CODIGO = "^CLI-\\d{6}$";
    private static final String PATRON_DPI = "^\\d{13}$";

    private final ClienteRepository clientes;
    private final IdentidadRepository identidades;
    private final DireccionRepository direcciones;
    private final MunicipioRepository municipios;
    private final DepartamentoRepository departamentos;
    private final GeneroRepository generos;
    private final UsuarioRepository usuarios;
    private final BitacoraConsultaRepository bitacora;
    private final ConsultaCuentasService consultaCuentas;
    private final Formato formato;

    public ConsultaClienteService(ClienteRepository clientes, IdentidadRepository identidades, DireccionRepository direcciones,
                                  MunicipioRepository municipios, DepartamentoRepository departamentos,
                                  GeneroRepository generos, UsuarioRepository usuarios, BitacoraConsultaRepository bitacora,
                                  ConsultaCuentasService consultaCuentas, Formato formato) {
        this.clientes = clientes;
        this.identidades = identidades;
        this.direcciones = direcciones;
        this.municipios = municipios;
        this.departamentos = departamentos;
        this.generos = generos;
        this.usuarios = usuarios;
        this.bitacora = bitacora;
        this.consultaCuentas = consultaCuentas;
        this.formato = formato;
    }

    @Transactional
    public Optional<Cliente> buscar(String criterio, UsuarioActual empleado) {
        String texto = criterio == null ? "" : criterio.trim().toUpperCase(Locale.ROOT);
        Optional<Cliente> encontrado;
        TipoConsulta tipo;
        String descripcion;
        if (texto.matches(PATRON_CODIGO)) {
            encontrado = clientes.findByCodigoCliente(texto);
            tipo = TipoConsulta.DATO_PERSONAL;
            descripcion = "Búsqueda por código " + texto;
        } else {
            String dpi = texto.replaceAll("[\\s-]", "");
            if (!dpi.matches(PATRON_DPI)) {
                throw new ReglaNegocioException("Escriba un DPI de 13 dígitos o un código de cliente como CLI-000001.");
            }
            encontrado = identidades.findByDpi(dpi).flatMap(i -> clientes.findByIdIdentidad(i.getIdIdentidad()));
            tipo = TipoConsulta.BUSQUEDA_DPI;
            descripcion = "Búsqueda por DPI " + formato.dpiOculto(dpi);
        }
        registrarConsulta(empleado, tipo, encontrado.map(Cliente::getIdCliente).orElse(null), descripcion,
                encontrado.isPresent() ? 1 : 0);
        return encontrado;
    }

    @Transactional
    public Optional<DetalleCliente> detalle(String codigoCliente, UsuarioActual empleado) {
        String codigo = codigoCliente == null ? "" : codigoCliente.trim().toUpperCase(Locale.ROOT);
        Optional<Cliente> encontrado = clientes.findByCodigoCliente(codigo);
        if (encontrado.isEmpty()) {
            return Optional.empty();
        }
        Cliente cliente = encontrado.get();
        Identidad identidad = identidades.findById(cliente.getIdIdentidad()).orElseThrow();
        Direccion direccion = direcciones.findById(identidad.getIdDireccion()).orElseThrow();
        Municipio municipio = municipios.findById(direccion.getIdMunicipio()).orElseThrow();
        String departamento = departamentos.findById(municipio.getIdDepartamento()).map(Departamento::getNombre).orElse("");
        String genero = generos.findById(identidad.getIdGenero()).map(g -> g.getNombre()).orElse("");
        return Optional.of(new DetalleCliente(cliente, identidad, NombreCompleto.de(identidad), genero,
                direccion.getDescripcionDireccion(), municipio.getNombre(), departamento,
                usuarios.findByIdCliente(cliente.getIdCliente()).orElse(null),
                consultaCuentas.deCliente(empleado.idSesionUsuario(), cliente.getIdCliente())));
    }

    private void registrarConsulta(UsuarioActual empleado, TipoConsulta tipo, Integer idCliente, String criterio, int cantidad) {
        BitacoraConsulta consulta = new BitacoraConsulta();
        consulta.setIdSesionUsuario(empleado.idSesionUsuario());
        consulta.setUsername(empleado.username());
        consulta.setTipoConsulta(tipo);
        consulta.setIdCliente(idCliente);
        consulta.setCriterio(criterio);
        consulta.setCantidadRegistros(cantidad);
        consulta.setFechaHora(LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
        bitacora.save(consulta);
    }
}
