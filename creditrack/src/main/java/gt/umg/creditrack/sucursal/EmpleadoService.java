package gt.umg.creditrack.sucursal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gt.umg.creditrack.comun.bd.GeneradorCodigos;
import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.comun.modelo.EstadoActivo;
import gt.umg.creditrack.comun.modelo.TipoRegistro;
import gt.umg.creditrack.comun.modelo.TipoUsuario;
import gt.umg.creditrack.persona.Identidad;
import gt.umg.creditrack.persona.NombreCompleto;
import gt.umg.creditrack.persona.RegistroPersonaService;
import gt.umg.creditrack.seguridad.GeneradorContrasena;
import gt.umg.creditrack.seguridad.UsuarioActual;
import gt.umg.creditrack.usuario.EstadoUsuario;
import gt.umg.creditrack.usuario.GeneradorUsername;
import gt.umg.creditrack.usuario.Rol;
import gt.umg.creditrack.usuario.RolRepository;
import gt.umg.creditrack.usuario.Usuario;
import gt.umg.creditrack.usuario.UsuarioRepository;

@Service
public class EmpleadoService {

    public record ResultadoRegistroEmpleado(String codigo, String nombre, String rol, String username, String contrasenaTemporal) {
    }

    public record FilaEmpleado(Integer idEmpleado, String codigo, String nombre, String puesto, String sucursal, Integer idSucursal,
                               String rol, Integer idRol, String username, String estado, LocalDate fechaIngreso) {
    }

    private static final String SQL_EMPLEADOS = """
            SELECT e.id_empleado, e.codigo_empleado, CONCAT_WS(' ', i.primer_nombre, i.primer_apellido, i.segundo_apellido) AS nombre,
                   e.puesto, s.nombre AS sucursal, e.id_sucursal, r.nombre AS rol, r.id_rol, u.username, e.estado, e.fecha_ingreso
              FROM empleado e
              JOIN identidad i ON i.id_identidad = e.id_identidad
              JOIN sucursal s ON s.id_sucursal = e.id_sucursal
              LEFT JOIN usuario u ON u.id_empleado = e.id_empleado
              LEFT JOIN rol r ON r.id_rol = u.id_rol
            """;

    private final RegistroPersonaService personas;
    private final EmpleadoRepository empleados;
    private final SucursalRepository sucursales;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final GeneradorCodigos generadorCodigos;
    private final GeneradorUsername generadorUsername;
    private final GeneradorContrasena generadorContrasena;
    private final PasswordEncoder codificador;
    private final JdbcTemplate jdbc;

    public EmpleadoService(RegistroPersonaService personas, EmpleadoRepository empleados, SucursalRepository sucursales,
                           UsuarioRepository usuarios, RolRepository roles, GeneradorCodigos generadorCodigos,
                           GeneradorUsername generadorUsername, GeneradorContrasena generadorContrasena,
                           PasswordEncoder codificador, JdbcTemplate jdbc) {
        this.personas = personas;
        this.empleados = empleados;
        this.sucursales = sucursales;
        this.usuarios = usuarios;
        this.roles = roles;
        this.generadorCodigos = generadorCodigos;
        this.generadorUsername = generadorUsername;
        this.generadorContrasena = generadorContrasena;
        this.codificador = codificador;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<Rol> rolesDeEmpleado() {
        return roles.findAll().stream()
                .filter(r -> r.getTipoUsuario() == TipoUsuario.EMPLEADO && Boolean.TRUE.equals(r.getActivo()))
                .sorted(java.util.Comparator.comparing(Rol::getNombre))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Sucursal> sucursalesActivas() {
        return sucursales.findAll().stream().filter(s -> s.getEstado() == EstadoSucursal.ACTIVA).toList();
    }

    public List<FilaEmpleado> listar() {
        return jdbc.query(SQL_EMPLEADOS + " ORDER BY e.estado, e.codigo_empleado", this::mapear);
    }

    public FilaEmpleado buscar(String codigo) {
        List<FilaEmpleado> filas = jdbc.query(SQL_EMPLEADOS + " WHERE e.codigo_empleado = ?", this::mapear,
                codigo == null ? "" : codigo.trim().toUpperCase());
        if (filas.isEmpty()) {
            throw new ReglaNegocioException("El empleado no existe.");
        }
        return filas.get(0);
    }

    @Transactional
    public ResultadoRegistroEmpleado registrar(RegistroEmpleadoForm form, UsuarioActual administrador) {
        personas.validar(form);
        Rol rol = rolEmpleado(form.getIdRol());
        Sucursal sucursal = sucursalActiva(form.getIdSucursal());
        Identidad identidad = personas.crear(form);

        Empleado empleado = new Empleado();
        empleado.setIdIdentidad(identidad.getIdIdentidad());
        empleado.setCodigoEmpleado(generadorCodigos.siguienteCodigoEmpleado());
        empleado.setIdSucursal(sucursal.getIdSucursal());
        empleado.setPuesto(form.getPuesto().trim());
        empleado.setFechaIngreso(form.getFechaIngreso());
        empleado.setEstado(EstadoActivo.ACTIVO);
        empleado.setIdUsuarioRegistro(administrador.idUsuario());
        empleado.setTipoRegistro(TipoRegistro.EMPLEADO);
        empleados.save(empleado);

        String temporal = generadorContrasena.temporal();
        Usuario usuario = new Usuario();
        usuario.setTipo(TipoUsuario.EMPLEADO);
        usuario.setIdEmpleado(empleado.getIdEmpleado());
        usuario.setIdRol(rol.getIdRol());
        usuario.setUsername(generadorUsername.para(identidad.getPrimerNombre(), identidad.getPrimerApellido()));
        usuario.setCorreo(form.getCorreo());
        usuario.setHashContrasena(codificador.encode(temporal));
        usuario.setRequiereCambioContrasena(Boolean.TRUE);
        usuario.setFechaUltimoCambio(LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
        usuario.setIdUsuarioCreador(administrador.idUsuario());
        usuarios.save(usuario);

        return new ResultadoRegistroEmpleado(empleado.getCodigoEmpleado(), NombreCompleto.de(identidad), rol.getNombre(),
                usuario.getUsername(), temporal);
    }

    @Transactional
    public void actualizar(String codigo, String puesto, Integer idSucursal, Integer idRol, UsuarioActual administrador) {
        Empleado empleado = empleadoActivo(codigo);
        if (puesto == null || puesto.isBlank() || puesto.trim().length() > 50) {
            throw new ReglaNegocioException("Escriba el puesto (máximo 50 caracteres).");
        }
        Sucursal sucursal = sucursalActiva(idSucursal);
        Rol rol = rolEmpleado(idRol);
        Usuario usuario = usuarios.findByIdEmpleado(empleado.getIdEmpleado()).orElse(null);
        if (usuario != null && empleado.getIdEmpleado().equals(administrador.idEmpleado()) && !usuario.getIdRol().equals(rol.getIdRol())) {
            throw new ReglaNegocioException("No puede cambiar su propio rol.");
        }
        empleado.setPuesto(puesto.trim());
        empleado.setIdSucursal(sucursal.getIdSucursal());
        if (usuario != null) {
            usuario.setIdRol(rol.getIdRol());
        }
    }

    @Transactional
    public void darDeBaja(String codigo, UsuarioActual administrador) {
        Empleado empleado = empleadoActivo(codigo);
        if (empleado.getIdEmpleado().equals(administrador.idEmpleado())) {
            throw new ReglaNegocioException("No puede darse de baja a sí mismo.");
        }
        LocalDate hoy = LocalDate.now(ZoneOffset.UTC);
        empleado.setEstado(EstadoActivo.INACTIVO);
        empleado.setFechaBaja(hoy.isBefore(empleado.getFechaIngreso()) ? empleado.getFechaIngreso() : hoy);
        usuarios.findByIdEmpleado(empleado.getIdEmpleado()).ifPresent(u -> u.setEstado(EstadoUsuario.INACTIVO));
    }

    private Empleado empleadoActivo(String codigo) {
        Empleado empleado = empleados.findByCodigoEmpleado(codigo == null ? "" : codigo.trim().toUpperCase())
                .orElseThrow(() -> new ReglaNegocioException("El empleado no existe."));
        if (empleado.getEstado() != EstadoActivo.ACTIVO) {
            throw new ReglaNegocioException("El empleado está dado de baja.");
        }
        return empleado;
    }

    private Rol rolEmpleado(Integer idRol) {
        return roles.findById(idRol == null ? -1 : idRol)
                .filter(r -> r.getTipoUsuario() == TipoUsuario.EMPLEADO && Boolean.TRUE.equals(r.getActivo()))
                .orElseThrow(() -> new ReglaNegocioException("Seleccione un rol de empleado válido."));
    }

    private Sucursal sucursalActiva(Integer idSucursal) {
        return sucursales.findById(idSucursal == null ? -1 : idSucursal)
                .filter(s -> s.getEstado() == EstadoSucursal.ACTIVA)
                .orElseThrow(() -> new ReglaNegocioException("Seleccione una sucursal activa."));
    }

    private FilaEmpleado mapear(java.sql.ResultSet f, int n) throws java.sql.SQLException {
        java.sql.Date ingreso = f.getDate("fecha_ingreso");
        return new FilaEmpleado(f.getInt("id_empleado"), f.getString("codigo_empleado"), f.getString("nombre"), f.getString("puesto"),
                f.getString("sucursal"), f.getInt("id_sucursal"), f.getString("rol"), (Integer) f.getObject("id_rol"),
                f.getString("username"), f.getString("estado"), ingreso == null ? null : ingreso.toLocalDate());
    }
}
