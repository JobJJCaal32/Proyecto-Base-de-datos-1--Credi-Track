package gt.umg.creditrack.comun.bd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import gt.umg.creditrack.PropiedadesPrueba;

class CifradoAesConverterTest {

    private final CifradoAesConverter cifrado = new CifradoAesConverter(PropiedadesPrueba.crear());

    @Test
    void cifraIgualQueAesEncryptDeMysql() {
        assertThat(cifrado.convertToDatabaseColumn("2508890128356")).isEqualTo("EBDB120A4A91F574BF5F4654AFAAEA90");
        assertThat(cifrado.convertToDatabaseColumn("5512-3408")).isEqualTo("36DA2BAA1078F014F3D905AA36AEE14E");
    }

    @Test
    void descifraLoQueGuardaMysql() {
        assertThat(cifrado.convertToEntityAttribute("EBDB120A4A91F574BF5F4654AFAAEA90")).isEqualTo("2508890128356");
    }

    @Test
    void idaYVuelta() {
        String valor = "Barrio San Marcos, Cobán";
        assertThat(cifrado.convertToEntityAttribute(cifrado.convertToDatabaseColumn(valor))).isEqualTo(valor);
    }

    @Test
    void nuloSigueSiendoNulo() {
        assertThat(cifrado.convertToDatabaseColumn(null)).isNull();
        assertThat(cifrado.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void textoQueNoEsCifradoFalla() {
        assertThatThrownBy(() -> cifrado.convertToEntityAttribute("ZZZ")).isInstanceOf(IllegalStateException.class);
    }
}
