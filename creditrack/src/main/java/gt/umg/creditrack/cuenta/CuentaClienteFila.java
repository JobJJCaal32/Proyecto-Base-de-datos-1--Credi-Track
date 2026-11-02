package gt.umg.creditrack.cuenta;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CuentaClienteFila(
        Integer idCuenta,
        String numeroCuenta,
        String alias,
        String tipoCuenta,
        String estadoCuenta,
        boolean permiteOperar,
        BigDecimal saldoActual,
        BigDecimal saldoRetenido,
        BigDecimal saldoDisponible,
        LocalDate fechaApertura,
        LocalDate fechaCierre,
        String sucursalApertura) {
}
