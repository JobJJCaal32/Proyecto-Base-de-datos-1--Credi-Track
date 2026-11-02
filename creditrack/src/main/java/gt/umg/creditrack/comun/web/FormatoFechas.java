package gt.umg.creditrack.comun.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import gt.umg.creditrack.config.CrediTrackProperties;

@Component("fechas")
public class FormatoFechas {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CrediTrackProperties propiedades;

    public FormatoFechas(CrediTrackProperties propiedades) {
        this.propiedades = propiedades;
    }

    public String fechaHora(LocalDateTime utc) {
        if (utc == null) {
            return "";
        }
        return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(propiedades.zona()).format(FECHA_HORA);
    }

    public String fecha(LocalDate fecha) {
        return fecha == null ? "" : fecha.format(FECHA);
    }
}
