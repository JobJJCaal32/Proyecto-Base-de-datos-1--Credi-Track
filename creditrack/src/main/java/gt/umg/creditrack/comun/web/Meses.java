package gt.umg.creditrack.comun.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import gt.umg.creditrack.config.CrediTrackProperties;

@Component
public class Meses {

    public record Opcion(String valor, String etiqueta) {
    }

    public record Rango(LocalDateTime desdeUtc, LocalDateTime hastaUtc) {
    }

    private static final DateTimeFormatter ETIQUETA = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.forLanguageTag("es-GT"));

    private final ZoneId zona;

    public Meses(CrediTrackProperties propiedades) {
        this.zona = propiedades.zona();
    }

    public List<Opcion> ultimos(int cantidad) {
        List<Opcion> opciones = new ArrayList<>();
        YearMonth mes = YearMonth.now(zona);
        for (int i = 0; i < cantidad; i++) {
            String texto = mes.format(ETIQUETA);
            opciones.add(new Opcion(mes.toString(), Character.toUpperCase(texto.charAt(0)) + texto.substring(1)));
            mes = mes.minusMonths(1);
        }
        return opciones;
    }

    public Rango rango(String valor) {
        if (valor == null || valor.isBlank()) {
            return new Rango(null, null);
        }
        try {
            YearMonth mes = YearMonth.parse(valor);
            return new Rango(utc(mes), utc(mes.plusMonths(1)));
        } catch (DateTimeParseException e) {
            return new Rango(null, null);
        }
    }

    public Rango rangoDias(LocalDate desde, LocalDate hasta) {
        return new Rango(desde == null ? null : utc(desde), hasta == null ? null : utc(hasta.plusDays(1)));
    }

    private LocalDateTime utc(LocalDate dia) {
        return dia.atStartOfDay(zona).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private LocalDateTime utc(YearMonth mes) {
        return mes.atDay(1).atStartOfDay(zona).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
