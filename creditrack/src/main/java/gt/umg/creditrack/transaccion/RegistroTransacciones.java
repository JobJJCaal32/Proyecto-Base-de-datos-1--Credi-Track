package gt.umg.creditrack.transaccion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import gt.umg.creditrack.catalogo.EstadoTransaccionRepository;
import gt.umg.creditrack.catalogo.TipoOperacionRepository;
import gt.umg.creditrack.comun.bd.GeneradorCodigos;

@Component
public class RegistroTransacciones {

    public static final String APLICADA = "APLICADA";
    public static final String PENDIENTE = "PENDIENTE";
    public static final String RECHAZADA = "RECHAZADA";
    public static final String REVERSADA = "REVERSADA";

    private final TransaccionRepository transacciones;
    private final TipoOperacionRepository tiposOperacion;
    private final EstadoTransaccionRepository estados;
    private final GeneradorCodigos generador;

    public RegistroTransacciones(TransaccionRepository transacciones, TipoOperacionRepository tiposOperacion,
                                 EstadoTransaccionRepository estados, GeneradorCodigos generador) {
        this.transacciones = transacciones;
        this.tiposOperacion = tiposOperacion;
        this.estados = estados;
        this.generador = generador;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Transaccion nueva(String operacion, String estado, BigDecimal monto, Canal canal, Long idSesionUsuario,
                             Integer idSesionCaja, String concepto, String referencia, boolean identidadVerificada,
                             Long idTransaccionOrigen) {
        Transaccion t = new Transaccion();
        t.setNumeroTransaccion(generador.siguienteNumeroTransaccion());
        t.setIdTipoOperacion(tiposOperacion.findByNombre(operacion).orElseThrow().getIdTipoOperacion());
        t.setIdEstadoTransaccion(idEstado(estado));
        t.setMonto(monto);
        t.setFechaHora(LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
        t.setCanal(canal);
        t.setIdSesionUsuario(idSesionUsuario);
        t.setIdSesionCaja(idSesionCaja);
        t.setConcepto(concepto);
        t.setReferencia(referencia);
        t.setIdentidadVerificada(identidadVerificada);
        t.setIdTransaccionOrigen(idTransaccionOrigen);
        return transacciones.save(t);
    }

    public Integer idEstado(String estado) {
        return estados.findByNombre(estado).orElseThrow().getIdEstadoTransaccion();
    }

    public String nombreEstado(Integer idEstado) {
        return estados.findById(idEstado).map(e -> e.getNombre()).orElse("");
    }

    public String nombreOperacion(Integer idTipoOperacion) {
        return tiposOperacion.findById(idTipoOperacion).map(o -> o.getNombre()).orElse("");
    }
}
