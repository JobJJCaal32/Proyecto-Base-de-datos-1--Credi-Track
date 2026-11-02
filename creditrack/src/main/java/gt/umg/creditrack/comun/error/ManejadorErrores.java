package gt.umg.creditrack.comun.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);
    private static final String MENSAJE_GENERICO = "Ocurrió un problema al procesar la operación. Intente de nuevo.";

    @ExceptionHandler(ReglaNegocioException.class)
    public ModelAndView reglaDeNegocio(ReglaNegocioException e) {
        return pagina(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
    }

    @ExceptionHandler(DataAccessException.class)
    public ModelAndView baseDeDatos(DataAccessException e) {
        String mensaje = ErroresBaseDatos.mensajeParaUsuario(e).orElse(null);
        if (mensaje == null) {
            log.error("Error de base de datos no controlado", e);
            return pagina(HttpStatus.INTERNAL_SERVER_ERROR, MENSAJE_GENERICO);
        }
        return pagina(HttpStatus.UNPROCESSABLE_CONTENT, mensaje);
    }

    private static ModelAndView pagina(HttpStatus estado, String mensaje) {
        ModelAndView vista = new ModelAndView("error/error");
        vista.setStatus(estado);
        vista.addObject("status", estado.value());
        vista.addObject("mensaje", mensaje);
        return vista;
    }
}
