package gt.umg.creditrack.usuario;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class GeneradorUsername {

    private static final int LONGITUD_MAXIMA = 30;
    private static final int LONGITUD_BASE = 26;

    private final UsuarioRepository usuarios;

    public GeneradorUsername(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public String para(String primerNombre, String primerApellido) {
        String base = normalizar(primerNombre) + "." + normalizar(primerApellido);
        if (base.length() > LONGITUD_BASE) {
            base = base.substring(0, LONGITUD_BASE);
        }
        String candidato = base;
        int sufijo = 2;
        while (usuarios.existsByUsername(candidato)) {
            candidato = base + sufijo;
            sufijo++;
        }
        return candidato.length() > LONGITUD_MAXIMA ? candidato.substring(0, LONGITUD_MAXIMA) : candidato;
    }

    private static String normalizar(String texto) {
        String sinAcentos = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String limpio = sinAcentos.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        return limpio.isEmpty() ? "cliente" : limpio;
    }
}
