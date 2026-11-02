package gt.umg.creditrack.supervision;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import gt.umg.creditrack.catalogo.TipoAlerta;
import gt.umg.creditrack.catalogo.TipoAlertaRepository;
import gt.umg.creditrack.comun.web.Formato;

@Service
public class AlertaService {

    public static final String MONTO_ALTO = "MONTO_ALTO";
    public static final String TRANSACCIONES_FRECUENTES = "TRANSACCIONES_FRECUENTES";

    private final TipoAlertaRepository tipos;
    private final AlertaRepository alertas;
    private final JdbcTemplate jdbc;
    private final Formato formato;

    public AlertaService(TipoAlertaRepository tipos, AlertaRepository alertas, JdbcTemplate jdbc, Formato formato) {
        this.tipos = tipos;
        this.alertas = alertas;
        this.jdbc = jdbc;
        this.formato = formato;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void evaluarMonto(Long idTransaccion, Integer idCuenta, String numeroCuenta, BigDecimal monto, String operacion) {
        LocalDateTime ahora = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
        tipos.findByNombre(MONTO_ALTO).filter(TipoAlerta::getActivo).ifPresent(tipo -> {
            if (tipo.getUmbralMonto() != null && monto.compareTo(tipo.getUmbralMonto()) >= 0) {
                crear(tipo, idTransaccion, idCuenta, ahora, operacion + " de " + formato.moneda(monto) + " en la cuenta "
                        + numeroCuenta + "; el límite de revisión es " + formato.moneda(tipo.getUmbralMonto()) + ".");
            }
        });
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void evaluarCargo(Long idTransaccion, Integer idCuenta, String numeroCuenta, BigDecimal monto, String operacion) {
        LocalDateTime ahora = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
        tipos.findByNombre(MONTO_ALTO).filter(TipoAlerta::getActivo).ifPresent(tipo -> {
            if (tipo.getUmbralMonto() != null && monto.compareTo(tipo.getUmbralMonto()) >= 0) {
                crear(tipo, idTransaccion, idCuenta, ahora, operacion + " de " + formato.moneda(monto) + " desde la cuenta "
                        + numeroCuenta + "; el límite de revisión es " + formato.moneda(tipo.getUmbralMonto()) + ".");
            }
        });
        tipos.findByNombre(TRANSACCIONES_FRECUENTES).filter(TipoAlerta::getActivo).ifPresent(tipo -> {
            if (tipo.getUmbralCantidad() == null || tipo.getVentanaMinutos() == null) {
                return;
            }
            LocalDateTime inicio = ahora.minusMinutes(tipo.getVentanaMinutos());
            Integer cargos = jdbc.queryForObject("""
                    SELECT COUNT(*)
                      FROM movimiento_cuenta m
                      JOIN transaccion t ON t.id_transaccion = m.id_transaccion
                     WHERE m.id_cuenta = ? AND m.naturaleza = 'CARGO' AND t.fecha_hora >= ?
                    """, Integer.class, idCuenta, inicio);
            Integer previas = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM alerta
                     WHERE id_tipo_alerta = ? AND id_cuenta = ? AND fecha_deteccion >= ?
                    """, Integer.class, tipo.getIdTipoAlerta(), idCuenta, inicio);
            if (cargos != null && cargos >= tipo.getUmbralCantidad() && previas != null && previas == 0) {
                crear(tipo, idTransaccion, idCuenta, ahora, cargos + " cargos a la cuenta " + numeroCuenta + " en los últimos "
                        + tipo.getVentanaMinutos() + " minutos.");
            }
        });
    }

    private void crear(TipoAlerta tipo, Long idTransaccion, Integer idCuenta, LocalDateTime ahora, String detalle) {
        Alerta alerta = new Alerta();
        alerta.setIdTipoAlerta(tipo.getIdTipoAlerta());
        alerta.setIdTransaccion(idTransaccion);
        alerta.setIdCuenta(idCuenta);
        alerta.setFechaDeteccion(ahora);
        alerta.setDetalle(detalle.length() > 300 ? detalle.substring(0, 300) : detalle);
        alerta.setEstado(EstadoAlerta.PENDIENTE);
        alertas.save(alerta);
    }
}
