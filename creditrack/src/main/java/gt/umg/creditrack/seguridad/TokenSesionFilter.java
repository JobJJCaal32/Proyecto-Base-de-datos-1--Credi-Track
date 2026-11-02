package gt.umg.creditrack.seguridad;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import gt.umg.creditrack.comun.bd.ContextoSesion;

public class TokenSesionFilter extends OncePerRequestFilter {

    public static final String ATRIBUTO_RESULTADO = "creditrack.sesion.resultado";
    public static final String RUTA_CAMBIO_CONTRASENA = "/cambiar-contrasena";

    private static final List<String> PREFIJOS_ESTATICOS = List.of("/css/", "/js/", "/img/");
    private static final Set<String> RUTAS_PERMITIDAS_SIN_CAMBIO = Set.of(RUTA_CAMBIO_CONTRASENA, "/logout", "/error");

    private final SesionService sesiones;
    private final CookieSesion cookie;

    public TokenSesionFilter(SesionService sesiones, CookieSesion cookie) {
        this.sesiones = sesiones;
        this.cookie = cookie;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = rutaSinContexto(request);
        return "/favicon.ico".equals(ruta) || PREFIJOS_ESTATICOS.stream().anyMatch(ruta::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        try {
            String token = cookie.leer(request);
            if (token != null) {
                ResultadoValidacion validacion = sesiones.validar(token);
                if (validacion.valida()) {
                    UsuarioActual usuario = validacion.usuario();
                    autenticar(usuario);
                    ContextoSesion.establecer(usuario.username(), usuario.idSesionUsuario());
                    if (usuario.requiereCambioContrasena() && !RUTAS_PERMITIDAS_SIN_CAMBIO.contains(rutaSinContexto(request))) {
                        response.sendRedirect(request.getContextPath() + RUTA_CAMBIO_CONTRASENA);
                        return;
                    }
                } else {
                    cookie.borrar(response);
                    request.setAttribute(ATRIBUTO_RESULTADO, validacion.resultado());
                }
            }
            cadena.doFilter(request, response);
        } finally {
            ContextoSesion.limpiar();
            SecurityContextHolder.clearContext();
        }
    }

    private void autenticar(UsuarioActual usuario) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + usuario.rol()));
        autoridades.add(new SimpleGrantedAuthority("TIPO_" + usuario.tipo().name()));
        usuario.permisos().forEach(p -> autoridades.add(new SimpleGrantedAuthority(p)));
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(usuario, null, autoridades));
        SecurityContextHolder.setContext(contexto);
    }

    private static String rutaSinContexto(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
