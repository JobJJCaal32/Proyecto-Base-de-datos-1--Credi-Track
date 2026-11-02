package gt.umg.creditrack.seguridad;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class GeneradorContrasena {

    private static final String MAYUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijkmnpqrstuvwxyz";
    private static final String NUMEROS = "23456789";
    private static final String SIMBOLOS = "#$%&*!@?";
    private static final String TODOS = MAYUSCULAS + MINUSCULAS + NUMEROS + SIMBOLOS;
    private static final int LONGITUD = 10;

    private final SecureRandom azar = new SecureRandom();

    public String temporal() {
        List<Character> caracteres = new ArrayList<>();
        caracteres.add(uno(MAYUSCULAS));
        caracteres.add(uno(MINUSCULAS));
        caracteres.add(uno(NUMEROS));
        caracteres.add(uno(SIMBOLOS));
        while (caracteres.size() < LONGITUD) {
            caracteres.add(uno(TODOS));
        }
        Collections.shuffle(caracteres, azar);
        StringBuilder resultado = new StringBuilder(LONGITUD);
        caracteres.forEach(resultado::append);
        return resultado.toString();
    }

    private char uno(String fuente) {
        return fuente.charAt(azar.nextInt(fuente.length()));
    }
}
