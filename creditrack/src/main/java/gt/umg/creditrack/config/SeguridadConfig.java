package gt.umg.creditrack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;

import gt.umg.creditrack.comun.modelo.TipoUsuario;
import gt.umg.creditrack.comun.web.menu.CatalogoMenu;
import gt.umg.creditrack.comun.web.menu.OpcionMenu;
import gt.umg.creditrack.seguridad.CookieSesion;
import gt.umg.creditrack.seguridad.ResultadoValidacion;
import gt.umg.creditrack.seguridad.SesionService;
import gt.umg.creditrack.seguridad.TokenSesionFilter;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SeguridadConfig {

    private static final String AUTORIDAD_TIPO = "TIPO_";

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, SesionService sesiones, CookieSesion cookie,
                                          CrediTrackProperties propiedades, CatalogoMenu catalogoMenu) throws Exception {
        CookieCsrfTokenRepository repositorioCsrf = new CookieCsrfTokenRepository();
        repositorioCsrf.setCookieCustomizer(c -> c.secure(propiedades.sesion().cookieSegura()).sameSite("Strict"));

        http
                .authorizeHttpRequests(reglas -> {
                    reglas.requestMatchers("/login", "/error", "/favicon.ico", "/css/**", "/js/**", "/img/**").permitAll();
                    for (OpcionMenu opcion : catalogoMenu.protegidasDeMasEspecificaAMenos()) {
                        reglas.requestMatchers(opcion.ruta(), opcion.ruta() + "/**")
                                .access(AuthorizationManagers.allOf(
                                        AuthorityAuthorizationManager.hasAuthority(AUTORIDAD_TIPO + opcion.tipo().name()),
                                        AuthorityAuthorizationManager.hasAuthority(opcion.permiso())));
                    }
                    reglas.requestMatchers(CatalogoMenu.INICIO_CLIENTE, CatalogoMenu.INICIO_CLIENTE + "/**")
                            .hasAuthority(AUTORIDAD_TIPO + TipoUsuario.CLIENTE.name());
                    reglas.requestMatchers(CatalogoMenu.INICIO_EMPLEADO, CatalogoMenu.INICIO_EMPLEADO + "/**")
                            .hasAuthority(AUTORIDAD_TIPO + TipoUsuario.EMPLEADO.name());
                    reglas.anyRequest().authenticated();
                })
                .addFilterBefore(new TokenSesionFilter(sesiones, cookie), AnonymousAuthenticationFilter.class)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(c -> c.requestCache(new NullRequestCache()))
                .csrf(c -> c.csrfTokenRepository(repositorioCsrf))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e.authenticationEntryPoint(entradaAlLogin()));
        return http.build();
    }

    private AuthenticationEntryPoint entradaAlLogin() {
        return (request, response, excepcion) -> {
            Object resultado = request.getAttribute(TokenSesionFilter.ATRIBUTO_RESULTADO);
            String aviso = "";
            if (ResultadoValidacion.EXPIRADA.equals(resultado)) {
                aviso = "?sesion=expirada";
            } else if (ResultadoValidacion.USUARIO_NO_ACTIVO.equals(resultado)) {
                aviso = "?sesion=bloqueada";
            } else if (resultado != null) {
                aviso = "?sesion=cerrada";
            }
            response.sendRedirect(request.getContextPath() + "/login" + aviso);
        };
    }
}
