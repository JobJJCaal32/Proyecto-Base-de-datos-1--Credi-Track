package gt.umg.creditrack.cuenta;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class AperturaCuentaForm {

    @NotBlank(message = "Escriba el código del cliente.")
    @Pattern(regexp = "^(?i)CLI-\\d{6}$", message = "El código tiene la forma CLI-000001.")
    private String codigoCliente;

    @NotNull(message = "Seleccione el tipo de cuenta.")
    private Integer idTipoCuenta;

    @NotNull(message = "Escriba el monto del depósito de apertura.")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero.")
    @DecimalMax(value = "9999999999999.99", message = "El monto es demasiado grande.")
    @Digits(integer = 13, fraction = 2, message = "Use como máximo 2 decimales.")
    private BigDecimal montoApertura;

    public String getCodigoCliente() {
        return codigoCliente;
    }

    public void setCodigoCliente(String codigoCliente) {
        this.codigoCliente = codigoCliente;
    }

    public Integer getIdTipoCuenta() {
        return idTipoCuenta;
    }

    public void setIdTipoCuenta(Integer idTipoCuenta) {
        this.idTipoCuenta = idTipoCuenta;
    }

    public BigDecimal getMontoApertura() {
        return montoApertura;
    }

    public void setMontoApertura(BigDecimal montoApertura) {
        this.montoApertura = montoApertura;
    }
}
