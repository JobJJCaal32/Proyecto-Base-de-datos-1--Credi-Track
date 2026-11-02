package gt.umg.creditrack.persona;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class NombreCompleto {

    private NombreCompleto() {
    }

    public static String de(Identidad identidad) {
        return Stream.of(identidad.getPrimerNombre(), identidad.getSegundoNombre(), identidad.getTercerNombre(),
                        identidad.getPrimerApellido(), identidad.getSegundoApellido())
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(" "));
    }
}
