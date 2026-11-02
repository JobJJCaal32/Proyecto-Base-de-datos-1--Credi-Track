package gt.umg.creditrack.cuenta;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.seguridad.UsuarioActual;

@Service
public class AliasCuentaService {

    private static final String PATRON_ALIAS = "^[\\p{L}\\p{N} .,'\\-]{1,50}$";

    private final CuentaRepository cuentas;

    public AliasCuentaService(CuentaRepository cuentas) {
        this.cuentas = cuentas;
    }

    @Transactional
    public void cambiar(UsuarioActual cliente, String numeroCuenta, String alias) {
        Cuenta cuenta = cuentas.findByNumeroCuenta(numeroCuenta)
                .filter(c -> c.getIdCliente().equals(cliente.idCliente()))
                .orElseThrow(() -> new ReglaNegocioException("La cuenta no existe o no es suya."));
        String limpio = alias == null ? null : alias.trim().replaceAll("\\s+", " ");
        if (limpio != null && limpio.isEmpty()) {
            limpio = null;
        }
        if (limpio != null && !limpio.matches(PATRON_ALIAS)) {
            throw new ReglaNegocioException("El alias admite hasta 50 letras, números, espacios y . , ' -");
        }
        cuenta.setAlias(limpio);
    }
}
