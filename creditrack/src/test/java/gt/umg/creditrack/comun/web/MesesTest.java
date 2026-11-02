package gt.umg.creditrack.comun.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import gt.umg.creditrack.PropiedadesPrueba;

class MesesTest {

    private final Meses meses = new Meses(PropiedadesPrueba.crear());

    @Test
    void unMesDeGuatemalaEnUtc() {
        Meses.Rango rango = meses.rango("2026-11");
        assertThat(rango.desdeUtc()).isEqualTo(LocalDateTime.of(2026, 11, 1, 6, 0));
        assertThat(rango.hastaUtc()).isEqualTo(LocalDateTime.of(2026, 12, 1, 6, 0));
    }

    @Test
    void unDiaCompletoIncluyeElDiaFinal() {
        Meses.Rango rango = meses.rangoDias(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 2));
        assertThat(rango.desdeUtc()).isEqualTo(LocalDateTime.of(2026, 11, 2, 6, 0));
        assertThat(rango.hastaUtc()).isEqualTo(LocalDateTime.of(2026, 11, 3, 6, 0));
    }

    @Test
    void sinFiltroNoHayLimites() {
        assertThat(meses.rango(null)).isEqualTo(new Meses.Rango(null, null));
        assertThat(meses.rango("noviembre")).isEqualTo(new Meses.Rango(null, null));
        assertThat(meses.rangoDias(null, null)).isEqualTo(new Meses.Rango(null, null));
    }

    @Test
    void ultimosMesesEmpiezaPorElActual() {
        assertThat(meses.ultimos(12)).hasSize(12);
    }
}
