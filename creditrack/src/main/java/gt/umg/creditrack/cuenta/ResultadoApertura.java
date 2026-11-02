package gt.umg.creditrack.cuenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ResultadoApertura(
        Integer idCuenta,
        String numeroCuenta,
        String tipoCuenta,
        BigDecimal monto,
        String numeroTransaccion,
        LocalDateTime fechaHora,
        String codigoCliente,
        String titular) {
}
