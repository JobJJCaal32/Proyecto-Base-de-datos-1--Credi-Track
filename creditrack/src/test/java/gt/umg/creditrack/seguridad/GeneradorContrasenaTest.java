package gt.umg.creditrack.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class GeneradorContrasenaTest {

    private final GeneradorContrasena generador = new GeneradorContrasena();

    @Test
    void cadaTemporalCumpleLaPolitica() {
        for (int i = 0; i < 500; i++) {
            String temporal = generador.temporal();
            assertThat(temporal).hasSize(10);
            assertThatCode(() -> PoliticaContrasena.validar(temporal, "usuario.prueba")).doesNotThrowAnyException();
        }
    }

    @Test
    void noUsaCaracteresQueSeConfunden() {
        for (int i = 0; i < 500; i++) {
            assertThat(generador.temporal()).doesNotContain("0", "O", "1", "l", "I");
        }
    }

    @Test
    void noSeRepiten() {
        Set<String> generadas = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            generadas.add(generador.temporal());
        }
        assertThat(generadas).hasSize(1000);
    }
}
