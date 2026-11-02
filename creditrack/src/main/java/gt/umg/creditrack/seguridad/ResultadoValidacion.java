package gt.umg.creditrack.seguridad;

public record ResultadoValidacion(String resultado, UsuarioActual usuario) {

    public static final String VALIDA = "VALIDA";
    public static final String NO_EXISTE = "NO_EXISTE";
    public static final String CERRADA = "CERRADA";
    public static final String EXPIRADA = "EXPIRADA";
    public static final String USUARIO_NO_ACTIVO = "USUARIO_NO_ACTIVO";

    public boolean valida() {
        return VALIDA.equals(resultado) && usuario != null;
    }
}
