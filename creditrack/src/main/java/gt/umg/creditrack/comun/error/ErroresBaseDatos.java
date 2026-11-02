package gt.umg.creditrack.comun.error;

import java.sql.SQLException;
import java.util.Optional;

public final class ErroresBaseDatos {

    private static final String SQLSTATE_REGLA = "45000";
    private static final String SQLSTATE_CHECK = "HY000";
    private static final int ERROR_CHECK = 3819;
    private static final int ERROR_DUPLICADO = 1062;

    private ErroresBaseDatos() {
    }

    public static Optional<String> mensajeParaUsuario(Throwable error) {
        for (Throwable actual = error; actual != null; actual = actual.getCause()) {
            if (actual instanceof ReglaNegocioException regla) {
                return Optional.ofNullable(regla.getMessage());
            }
            if (actual instanceof SQLException sql) {
                if (SQLSTATE_REGLA.equals(sql.getSQLState())) {
                    return Optional.ofNullable(sql.getMessage());
                }
                if (sql.getErrorCode() == ERROR_DUPLICADO) {
                    return Optional.of("Ya existe un registro con esos datos");
                }
                if (sql.getErrorCode() == ERROR_CHECK && SQLSTATE_CHECK.equals(sql.getSQLState())) {
                    return Optional.of("Los datos no cumplen las reglas de la base de datos");
                }
            }
            if (actual.getCause() == actual) {
                break;
            }
        }
        return Optional.empty();
    }
}
