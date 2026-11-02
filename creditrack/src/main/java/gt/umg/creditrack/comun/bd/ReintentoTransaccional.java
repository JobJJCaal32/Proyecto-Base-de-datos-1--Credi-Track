package gt.umg.creditrack.comun.bd;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class ReintentoTransaccional {

    private static final Logger log = LoggerFactory.getLogger(ReintentoTransaccional.class);
    private static final int INTENTOS = 5;

    public <T> T ejecutar(Supplier<T> operacion) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("El reintento debe envolver la transacción completa, no ejecutarse dentro de una.");
        }
        for (int intento = 1; ; intento++) {
            try {
                return operacion.get();
            } catch (PessimisticLockingFailureException | DuplicateKeyException e) {
                if (intento >= INTENTOS) {
                    throw e;
                }
                log.info("Conflicto de concurrencia en el intento {}; se repite la operación: {}", intento, e.getMessage());
                esperar(intento);
            }
        }
    }

    private static void esperar(int intento) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(20, 60) * intento);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operación interrumpida", e);
        }
    }
}
