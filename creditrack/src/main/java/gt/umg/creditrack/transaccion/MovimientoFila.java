package gt.umg.creditrack.transaccion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoFila(
        Long idMovimiento,
        Long idTransaccion,
        String numeroTransaccion,
        LocalDateTime fechaHora,
        String tipoOperacion,
        String estadoTransaccion,
        String canal,
        String concepto,
        String referencia,
        Integer idCuenta,
        String numeroCuenta,
        Integer idCliente,
        String codigoCliente,
        String titular,
        String naturaleza,
        BigDecimal monto,
        BigDecimal saldoAnterior,
        BigDecimal saldoPosterior,
        Integer orden,
        String cuentaContraparte,
        String titularContraparte,
        String realizadoPor) {

    public boolean esCargo() {
        return "CARGO".equals(naturaleza);
    }
}
