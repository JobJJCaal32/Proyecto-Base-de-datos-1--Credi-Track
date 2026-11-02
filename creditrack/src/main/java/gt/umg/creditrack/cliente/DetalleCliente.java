package gt.umg.creditrack.cliente;

import java.util.List;

import gt.umg.creditrack.cuenta.CuentaClienteFila;
import gt.umg.creditrack.persona.Identidad;
import gt.umg.creditrack.usuario.Usuario;

public record DetalleCliente(
        Cliente cliente,
        Identidad identidad,
        String nombreCompleto,
        String genero,
        String direccion,
        String municipio,
        String departamento,
        Usuario usuario,
        List<CuentaClienteFila> cuentas) {
}
