package gt.umg.creditrack.usuario;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.comun.modelo.TipoUsuario;
import gt.umg.creditrack.seguridad.GeneradorContrasena;

@Service
public class AdministracionUsuariosService {

    public record FilaUsuario(Integer idUsuario, String username, String tipo, String rol, String nombre, String estado,
                              Integer intentosFallidos, LocalDateTime fechaBloqueo, boolean requiereCambio) {
    }

    private static final String SQL_USUARIOS = """
            SELECT u.id_usuario, u.username, u.tipo, r.nombre AS rol, u.estado, u.intentos_fallidos, u.fecha_bloqueo,
                   u.requiere_cambio_contrasena,
                   CONCAT_WS(' ', i.primer_nombre, i.primer_apellido) AS nombre
              FROM usuario u
              JOIN rol r ON r.id_rol = u.id_rol
              LEFT JOIN cliente c ON c.id_cliente = u.id_cliente
              LEFT JOIN empleado e ON e.id_empleado = u.id_empleado
              LEFT JOIN identidad i ON i.id_identidad = COALESCE(c.id_identidad, e.id_identidad)
             WHERE u.tipo <> 'SISTEMA'
            """;

    private final JdbcTemplate jdbc;
    private final UsuarioRepository usuarios;
    private final GeneradorContrasena generador;
    private final PasswordEncoder codificador;

    public AdministracionUsuariosService(JdbcTemplate jdbc, UsuarioRepository usuarios, GeneradorContrasena generador,
                                         PasswordEncoder codificador) {
        this.jdbc = jdbc;
        this.usuarios = usuarios;
        this.generador = generador;
        this.codificador = codificador;
    }

    public List<FilaUsuario> buscar(String texto) {
        if (texto == null || texto.isBlank()) {
            return jdbc.query(SQL_USUARIOS + " AND u.estado = 'BLOQUEADO' ORDER BY u.fecha_bloqueo DESC LIMIT 50", this::mapear);
        }
        String patron = "%" + texto.trim().replace("\\", "").replace("%", "").replace("_", "\\_") + "%";
        return jdbc.query(SQL_USUARIOS + " AND (u.username LIKE ? OR CONCAT_WS(' ', i.primer_nombre, i.primer_apellido) LIKE ?)"
                + " ORDER BY u.username LIMIT 50", this::mapear, patron, patron);
    }

    @Transactional
    public void desbloquear(String username) {
        Usuario usuario = administrable(username);
        if (usuario.getEstado() != EstadoUsuario.BLOQUEADO) {
            throw new ReglaNegocioException("El usuario " + usuario.getUsername() + " no está bloqueado.");
        }
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setIntentosFallidos(0);
        usuario.setFechaBloqueo(null);
    }

    @Transactional
    public String resetearContrasena(String username) {
        Usuario usuario = administrable(username);
        String temporal = generador.temporal();
        usuario.setHashContrasena(codificador.encode(temporal));
        usuario.setRequiereCambioContrasena(Boolean.TRUE);
        usuario.setFechaUltimoCambio(LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
        if (usuario.getEstado() == EstadoUsuario.BLOQUEADO) {
            usuario.setEstado(EstadoUsuario.ACTIVO);
            usuario.setIntentosFallidos(0);
            usuario.setFechaBloqueo(null);
        }
        return temporal;
    }

    private Usuario administrable(String username) {
        Usuario usuario = usuarios.findByUsername(username == null ? "" : username.trim())
                .orElseThrow(() -> new ReglaNegocioException("El usuario no existe."));
        if (usuario.getTipo() == TipoUsuario.SISTEMA) {
            throw new ReglaNegocioException("El usuario del sistema no se administra desde aquí.");
        }
        return usuario;
    }

    private FilaUsuario mapear(java.sql.ResultSet fila, int n) throws java.sql.SQLException {
        java.sql.Timestamp bloqueo = fila.getTimestamp("fecha_bloqueo");
        return new FilaUsuario(fila.getInt("id_usuario"), fila.getString("username"), fila.getString("tipo"),
                fila.getString("rol"), fila.getString("nombre"), fila.getString("estado"), fila.getInt("intentos_fallidos"),
                bloqueo == null ? null : bloqueo.toLocalDateTime(), fila.getBoolean("requiere_cambio_contrasena"));
    }
}
