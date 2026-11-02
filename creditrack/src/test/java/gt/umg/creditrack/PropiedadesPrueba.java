package gt.umg.creditrack;

import java.math.BigDecimal;

import gt.umg.creditrack.config.CrediTrackProperties;

public final class PropiedadesPrueba {

    private PropiedadesPrueba() {
    }

    public static CrediTrackProperties crear() {
        return new CrediTrackProperties("America/Guatemala",
                new CrediTrackProperties.Seguridad("CrediTrack#2026!", 3),
                new CrediTrackProperties.Sesion("CT_SESION", 15, false),
                new CrediTrackProperties.Caja(new BigDecimal("10000.00")));
    }
}
