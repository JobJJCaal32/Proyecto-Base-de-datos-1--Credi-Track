package gt.umg.creditrack.caja;

import java.math.BigDecimal;

public record ResumenCaja(
        SesionCaja sesion,
        String sucursal,
        BigDecimal depositos,
        long cantidadDepositos,
        BigDecimal retiros,
        long cantidadRetiros,
        long transferencias,
        long pendientes,
        BigDecimal efectivoEsperado) {
}
