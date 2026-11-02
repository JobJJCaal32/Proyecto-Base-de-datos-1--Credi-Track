package gt.umg.creditrack.seguridad;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import gt.umg.creditrack.config.CrediTrackProperties;

@Component
public class CookieSesion {

    private final String nombre;
    private final boolean segura;

    public CookieSesion(CrediTrackProperties propiedades) {
        this.nombre = propiedades.sesion().nombreCookie();
        this.segura = propiedades.sesion().cookieSegura();
    }

    public String leer(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (nombre.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public void escribir(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, construir(token, -1).toString());
    }

    public void borrar(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, construir("", 0).toString());
    }

    private ResponseCookie construir(String valor, long segundos) {
        return ResponseCookie.from(nombre, valor)
                .httpOnly(true)
                .secure(segura)
                .sameSite("Strict")
                .path("/")
                .maxAge(segundos)
                .build();
    }
}
