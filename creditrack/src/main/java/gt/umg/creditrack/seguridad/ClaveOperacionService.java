package gt.umg.creditrack.seguridad;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.usuario.Usuario;
import gt.umg.creditrack.usuario.UsuarioRepository;

@Service
public class ClaveOperacionService {

    private static final String SECUENCIA = "01234567890";
    private static final String SECUENCIA_INVERSA = "09876543210";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;

    public ClaveOperacionService(UsuarioRepository usuarios, PasswordEncoder codificador) {
        this.usuarios = usuarios;
        this.codificador = codificador;
    }

    @Transactional(readOnly = true)
    public boolean configurada(UsuarioActual usuario) {
        return usuarios.findById(usuario.idUsuario()).map(u -> u.getHashClaveOperacion() != null).orElse(false);
    }

    @Transactional
    public void configurar(UsuarioActual actual, String contrasena, String clave, String confirmacion) {
        Usuario usuario = usuarios.findById(actual.idUsuario()).orElseThrow();
        if (contrasena == null || !codificador.matches(contrasena, usuario.getHashContrasena())) {
            throw new ReglaNegocioException("Su contraseña de ingreso no es correcta.");
        }
        if (clave == null || !clave.matches("^\\d{6}$")) {
            throw new ReglaNegocioException("La clave de operación debe tener exactamente 6 dígitos.");
        }
        if (!clave.equals(confirmacion)) {
            throw new ReglaNegocioException("La confirmación no coincide con la clave.");
        }
        if (clave.chars().distinct().count() == 1 || SECUENCIA.contains(clave) || SECUENCIA_INVERSA.contains(clave)) {
            throw new ReglaNegocioException("Elija una clave menos predecible: no use dígitos repetidos ni seguidos.");
        }
        usuario.setHashClaveOperacion(codificador.encode(clave));
        usuario.setFechaUltimoCambio(LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
    }
}
