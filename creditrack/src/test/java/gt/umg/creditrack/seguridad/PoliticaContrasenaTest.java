package gt.umg.creditrack.seguridad;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import gt.umg.creditrack.comun.error.ReglaNegocioException;

class PoliticaContrasenaTest {

    @Test
    void aceptaUnaContrasenaQueCumpleTodo() {
        assertThatCode(() -> PoliticaContrasena.validar("MiClave#2026", "marta.coc")).doesNotThrowAnyException();
    }

    @Test
    void rechazaLasQueNoCumplen() {
        rechaza("Ab#1", "entre 8 y 64");
        rechaza("A".repeat(60) + "b#12345", "entre 8 y 64");
        rechaza("miclave#2026", "mayúscula");
        rechaza("MICLAVE#2026", "minúscula");
        rechaza("MiClave#Sin", "número");
        rechaza("MiClave2026", "símbolo");
    }

    @Test
    void rechazaLaQueContieneElUsuario() {
        assertThatThrownBy(() -> PoliticaContrasena.validar("Marta.Coc#2026", "marta.coc"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("usuario");
    }

    @Test
    void rechazaNula() {
        assertThatThrownBy(() -> PoliticaContrasena.validar(null, "marta.coc")).isInstanceOf(ReglaNegocioException.class);
    }

    private static void rechaza(String contrasena, String motivo) {
        assertThatThrownBy(() -> PoliticaContrasena.validar(contrasena, "usuario"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining(motivo);
    }
}
