package gt.umg.creditrack.comun.web;

import java.math.BigDecimal;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component("formato")
public class Formato {

    public String moneda(BigDecimal monto) {
        if (monto == null) {
            return "";
        }
        return String.format(Locale.US, "Q %,.2f", monto);
    }

    public String dpiOculto(String dpi) {
        if (dpi == null || dpi.length() <= 4) {
            return dpi;
        }
        return "*".repeat(dpi.length() - 4) + dpi.substring(dpi.length() - 4);
    }
}
