package gt.umg.creditrack.comun.bd;

import java.util.Optional;

public final class ContextoSesion {

    public record Datos(String username, Long idSesionUsuario) {
    }

    private static final ThreadLocal<Datos> ACTUAL = new ThreadLocal<>();

    private ContextoSesion() {
    }

    public static void establecer(String username, Long idSesionUsuario) {
        ACTUAL.set(new Datos(username, idSesionUsuario));
    }

    public static Optional<Datos> actual() {
        return Optional.ofNullable(ACTUAL.get());
    }

    public static void limpiar() {
        ACTUAL.remove();
    }
}
