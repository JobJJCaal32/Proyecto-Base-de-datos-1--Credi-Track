package gt.umg.creditrack.seguridad;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import gt.umg.creditrack.comun.error.ErroresBaseDatos;
import gt.umg.creditrack.comun.modelo.TipoUsuario;
import gt.umg.creditrack.config.CrediTrackProperties;
import gt.umg.creditrack.usuario.EstadoUsuario;
import gt.umg.creditrack.usuario.Usuario;
import gt.umg.creditrack.usuario.UsuarioRepository;

@Service
public class SesionService {

    private static final Logger log = LoggerFactory.getLogger(SesionService.class);

    private static final String MOTIVO_NO_EXISTE = "USUARIO_NO_EXISTE";
    private static final String MOTIVO_BLOQUEADO = "USUARIO_BLOQUEADO";
    private static final String MOTIVO_INACTIVO = "USUARIO_INACTIVO";
    private static final String MOTIVO_CONTRASENA = "CONTRASENA_INCORRECTA";

    private static final String MENSAJE_CREDENCIALES = "Usuario o contraseña incorrectos.";
    private static final String MENSAJE_BLOQUEADO = "Su usuario está bloqueado. Comuníquese con el banco para desbloquearlo.";
    private static final String MENSAJE_INACTIVO = "Su usuario está inactivo. Comuníquese con el banco.";
    private static final String MENSAJE_RECIEN_BLOQUEADO =
            "Superó el número de intentos permitidos y su usuario quedó bloqueado. Comuníquese con el banco.";

    private static final String SQL_PERMISOS = """
            SELECT p.nombre
              FROM rol r
              JOIN rol_permiso rp ON rp.id_rol = r.id_rol
              JOIN permiso p ON p.id_permiso = rp.id_permiso
             WHERE r.nombre = ?
             ORDER BY p.nombre
            """;

    private final JdbcTemplate jdbc;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;
    private final CrediTrackProperties propiedades;
    private final String hashFicticio;

    public SesionService(JdbcTemplate jdbc, UsuarioRepository usuarios, PasswordEncoder codificador,
                         CrediTrackProperties propiedades) {
        this.jdbc = jdbc;
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.propiedades = propiedades;
        this.hashFicticio = codificador.encode(UUID.randomUUID().toString());
    }

    public ResultadoLogin iniciarSesion(String username, String contrasena, String direccionIp, String nombreEquipo) {
        String nombre = username == null ? "" : username.trim();
        String clave = contrasena == null ? "" : contrasena;
        if (nombre.isEmpty() || clave.isEmpty()) {
            return ResultadoLogin.fallo("Ingrese su usuario y su contraseña.");
        }
        if (nombre.length() > 30) {
            nombre = nombre.substring(0, 30);
        }

        Usuario usuario = usuarios.findByUsername(nombre).orElse(null);
        String motivo = motivoDeFallo(usuario, clave);

        try {
            Map<String, Object> fila = jdbc.queryForMap("CALL sp_registrar_login(?, ?, ?, ?, ?, ?)",
                    nombre, motivo == null ? 1 : 0, motivo, recortar(direccionIp, 45), recortar(nombreEquipo, 60),
                    propiedades.seguridad().maxIntentos());
            if (motivo == null) {
                return ResultadoLogin.exito((String) fila.get("token"), usuario.getRequiereCambioContrasena());
            }
            return ResultadoLogin.fallo(mensajeDeFallo(motivo, fila));
        } catch (DataAccessException e) {
            log.warn("No se pudo registrar el inicio de sesión de {}: {}", nombre, e.getMessage());
            return ResultadoLogin.fallo(ErroresBaseDatos.mensajeParaUsuario(e).orElse(MENSAJE_CREDENCIALES));
        }
    }

    public ResultadoValidacion validar(String token) {
        if (token == null || !token.matches("[0-9a-f]{64}")) {
            return new ResultadoValidacion(ResultadoValidacion.NO_EXISTE, null);
        }
        Map<String, Object> fila = jdbc.queryForMap("CALL sp_validar_sesion(?, ?)",
                token, propiedades.sesion().minutosInactividad());
        String resultado = (String) fila.get("resultado");
        if (!ResultadoValidacion.VALIDA.equals(resultado)) {
            return new ResultadoValidacion(resultado, null);
        }
        String rol = (String) fila.get("rol");
        Set<String> permisos = new LinkedHashSet<>(jdbc.queryForList(SQL_PERMISOS, String.class, rol));
        UsuarioActual usuario = new UsuarioActual(
                numeroLargo(fila.get("id_sesion_usuario")),
                numeroEntero(fila.get("id_usuario")),
                (String) fila.get("username"),
                TipoUsuario.valueOf((String) fila.get("tipo")),
                rol,
                numeroEntero(fila.get("id_cliente")),
                numeroEntero(fila.get("id_empleado")),
                verdadero(fila.get("requiere_cambio_contrasena")),
                (String) fila.get("nombre"),
                Set.copyOf(permisos));
        return new ResultadoValidacion(resultado, usuario);
    }

    public void cerrarSesion(String token, MotivoCierreSesion motivo) {
        if (token == null || !token.matches("[0-9a-f]{64}")) {
            return;
        }
        jdbc.queryForList("CALL sp_cerrar_sesion(?, ?)", token, motivo.name());
    }

    private String motivoDeFallo(Usuario usuario, String clave) {
        if (usuario == null || usuario.getTipo() == TipoUsuario.SISTEMA || usuario.getHashContrasena() == null) {
            codificador.matches(clave, hashFicticio);
            return MOTIVO_NO_EXISTE;
        }
        if (usuario.getEstado() == EstadoUsuario.BLOQUEADO) {
            return MOTIVO_BLOQUEADO;
        }
        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
            return MOTIVO_INACTIVO;
        }
        if (!codificador.matches(clave, usuario.getHashContrasena())) {
            return MOTIVO_CONTRASENA;
        }
        return null;
    }

    private String mensajeDeFallo(String motivo, Map<String, Object> fila) {
        return switch (motivo) {
            case MOTIVO_BLOQUEADO -> MENSAJE_BLOQUEADO;
            case MOTIVO_INACTIVO -> MENSAJE_INACTIVO;
            case MOTIVO_CONTRASENA -> {
                if (EstadoUsuario.BLOQUEADO.name().equals(fila.get("estado_usuario"))) {
                    yield MENSAJE_RECIEN_BLOQUEADO;
                }
                Integer restantes = numeroEntero(fila.get("intentos_restantes"));
                yield restantes == null ? MENSAJE_CREDENCIALES
                        : MENSAJE_CREDENCIALES + " Le quedan " + restantes + (restantes == 1 ? " intento." : " intentos.");
            }
            default -> MENSAJE_CREDENCIALES;
        };
    }

    private static String recortar(String valor, int maximo) {
        if (valor == null) {
            return null;
        }
        return valor.length() > maximo ? valor.substring(0, maximo) : valor;
    }

    private static Integer numeroEntero(Object valor) {
        return valor == null ? null : ((Number) valor).intValue();
    }

    private static Long numeroLargo(Object valor) {
        return valor == null ? null : ((Number) valor).longValue();
    }

    private static boolean verdadero(Object valor) {
        if (valor instanceof Boolean b) {
            return b;
        }
        return valor instanceof Number n && n.intValue() == 1;
    }
}
