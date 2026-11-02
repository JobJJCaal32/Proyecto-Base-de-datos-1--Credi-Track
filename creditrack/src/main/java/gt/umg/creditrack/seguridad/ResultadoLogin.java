package gt.umg.creditrack.seguridad;

public record ResultadoLogin(boolean exitoso, String token, boolean requiereCambioContrasena, String mensaje) {

    public static ResultadoLogin exito(String token, boolean requiereCambioContrasena) {
        return new ResultadoLogin(true, token, requiereCambioContrasena, null);
    }

    public static ResultadoLogin fallo(String mensaje) {
        return new ResultadoLogin(false, null, false, mensaje);
    }
}
