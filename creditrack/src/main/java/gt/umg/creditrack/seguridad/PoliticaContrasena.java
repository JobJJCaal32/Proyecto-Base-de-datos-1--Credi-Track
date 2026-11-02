package gt.umg.creditrack.seguridad;

import java.util.Locale;

import gt.umg.creditrack.comun.error.ReglaNegocioException;

public final class PoliticaContrasena {

    public static final int LONGITUD_MINIMA = 8;
    public static final int LONGITUD_MAXIMA = 64;
    public static final String DESCRIPCION =
            "Entre 8 y 64 caracteres, con al menos una mayúscula, una minúscula, un número y un símbolo, y sin contener su usuario.";

    private PoliticaContrasena() {
    }

    public static void validar(String contrasena, String username) {
        if (contrasena == null || contrasena.length() < LONGITUD_MINIMA || contrasena.length() > LONGITUD_MAXIMA) {
            throw new ReglaNegocioException("La contraseña debe tener entre 8 y 64 caracteres.");
        }
        if (contrasena.chars().noneMatch(Character::isUpperCase)) {
            throw new ReglaNegocioException("La contraseña debe tener al menos una letra mayúscula.");
        }
        if (contrasena.chars().noneMatch(Character::isLowerCase)) {
            throw new ReglaNegocioException("La contraseña debe tener al menos una letra minúscula.");
        }
        if (contrasena.chars().noneMatch(Character::isDigit)) {
            throw new ReglaNegocioException("La contraseña debe tener al menos un número.");
        }
        if (contrasena.chars().allMatch(Character::isLetterOrDigit)) {
            throw new ReglaNegocioException("La contraseña debe tener al menos un símbolo, por ejemplo # $ ! o @.");
        }
        if (username != null && !username.isBlank()
                && contrasena.toLowerCase(Locale.ROOT).contains(username.toLowerCase(Locale.ROOT))) {
            throw new ReglaNegocioException("La contraseña no puede contener su nombre de usuario.");
        }
    }
}
