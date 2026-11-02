package gt.umg.creditrack.cliente;

import gt.umg.creditrack.cuenta.ResultadoApertura;

public record ResultadoRegistro(
        String codigoCliente,
        String nombreCompleto,
        String correo,
        String username,
        String contrasenaTemporal,
        ResultadoApertura cuenta) {
}
