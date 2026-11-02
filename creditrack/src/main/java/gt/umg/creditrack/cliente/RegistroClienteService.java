package gt.umg.creditrack.cliente;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gt.umg.creditrack.comun.bd.GeneradorCodigos;
import gt.umg.creditrack.comun.modelo.TipoRegistro;
import gt.umg.creditrack.comun.modelo.TipoUsuario;
import gt.umg.creditrack.cuenta.AperturaCuentaService;
import gt.umg.creditrack.cuenta.ResultadoApertura;
import gt.umg.creditrack.persona.Identidad;
import gt.umg.creditrack.persona.NombreCompleto;
import gt.umg.creditrack.persona.RegistroPersonaService;
import gt.umg.creditrack.seguridad.GeneradorContrasena;
import gt.umg.creditrack.seguridad.UsuarioActual;
import gt.umg.creditrack.usuario.GeneradorUsername;
import gt.umg.creditrack.usuario.RolRepository;
import gt.umg.creditrack.usuario.Usuario;
import gt.umg.creditrack.usuario.UsuarioRepository;

@Service
public class RegistroClienteService {

    private static final String ROL_CLIENTE = "CLIENTE";

    private final RegistroPersonaService personas;
    private final ClienteRepository clientes;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final AperturaCuentaService aperturaCuenta;
    private final GeneradorCodigos generadorCodigos;
    private final GeneradorUsername generadorUsername;
    private final GeneradorContrasena generadorContrasena;
    private final PasswordEncoder codificador;

    public RegistroClienteService(RegistroPersonaService personas, ClienteRepository clientes, UsuarioRepository usuarios,
                                  RolRepository roles, AperturaCuentaService aperturaCuenta, GeneradorCodigos generadorCodigos,
                                  GeneradorUsername generadorUsername, GeneradorContrasena generadorContrasena,
                                  PasswordEncoder codificador) {
        this.personas = personas;
        this.clientes = clientes;
        this.usuarios = usuarios;
        this.roles = roles;
        this.aperturaCuenta = aperturaCuenta;
        this.generadorCodigos = generadorCodigos;
        this.generadorUsername = generadorUsername;
        this.generadorContrasena = generadorContrasena;
        this.codificador = codificador;
    }

    @Transactional
    public ResultadoRegistro registrar(RegistroClienteForm form, UsuarioActual oficial) {
        personas.validar(form);
        aperturaCuenta.validarApertura(form.getIdTipoCuenta(), form.getMontoApertura());
        Identidad identidad = personas.crear(form);

        Cliente cliente = new Cliente();
        cliente.setIdIdentidad(identidad.getIdIdentidad());
        cliente.setCodigoCliente(generadorCodigos.siguienteCodigoCliente());
        cliente.setFechaAlta(LocalDate.now(ZoneOffset.UTC));
        cliente.setIdUsuarioRegistro(oficial.idUsuario());
        cliente.setTipoRegistro(TipoRegistro.EMPLEADO);
        clientes.save(cliente);

        ResultadoApertura cuenta = aperturaCuenta.abrir(cliente.getIdCliente(), form.getIdTipoCuenta(), form.getMontoApertura(), oficial);

        String contrasenaTemporal = generadorContrasena.temporal();
        Usuario usuario = new Usuario();
        usuario.setTipo(TipoUsuario.CLIENTE);
        usuario.setIdCliente(cliente.getIdCliente());
        usuario.setIdRol(roles.findByNombre(ROL_CLIENTE).orElseThrow().getIdRol());
        usuario.setUsername(generadorUsername.para(identidad.getPrimerNombre(), identidad.getPrimerApellido()));
        usuario.setCorreo(form.getCorreo());
        usuario.setHashContrasena(codificador.encode(contrasenaTemporal));
        usuario.setRequiereCambioContrasena(Boolean.TRUE);
        usuario.setFechaUltimoCambio(LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
        usuario.setIdUsuarioCreador(oficial.idUsuario());
        usuarios.save(usuario);

        return new ResultadoRegistro(cliente.getCodigoCliente(), NombreCompleto.de(identidad), usuario.getCorreo(),
                usuario.getUsername(), contrasenaTemporal, cuenta);
    }
}
