package gt.umg.creditrack.seguridad;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.usuario.Usuario;
import gt.umg.creditrack.usuario.UsuarioRepository;

@Service
public class CambioContrasenaService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;

    public CambioContrasenaService(UsuarioRepository usuarios, PasswordEncoder codificador) {
        this.usuarios = usuarios;
        this.codificador = codificador;
    }

    @Transactional
    public void cambiar(Integer idUsuario, String actual, String nueva, String confirmacion) {
        Usuario usuario = usuarios.findById(idUsuario)
                .orElseThrow(() -> new ReglaNegocioException("El usuario ya no existe."));
        if (actual == null || !codificador.matches(actual, usuario.getHashContrasena())) {
            throw new ReglaNegocioException("La contraseña actual no es correcta.");
        }
        if (nueva == null || !nueva.equals(confirmacion)) {
            throw new ReglaNegocioException("La confirmación no coincide con la nueva contraseña.");
        }
        PoliticaContrasena.validar(nueva, usuario.getUsername());
        if (codificador.matches(nueva, usuario.getHashContrasena())) {
            throw new ReglaNegocioException("La nueva contraseña debe ser distinta de la actual.");
        }
        usuario.setHashContrasena(codificador.encode(nueva));
        usuario.setRequiereCambioContrasena(Boolean.FALSE);
        usuario.setFechaUltimoCambio(LocalDateTime.now(ZoneOffset.UTC));
    }
}
