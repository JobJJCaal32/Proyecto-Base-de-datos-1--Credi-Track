package gt.umg.creditrack.comun.bd;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.springframework.stereotype.Component;

import gt.umg.creditrack.config.CrediTrackProperties;

@Component
@Converter
public class CifradoAesConverter implements AttributeConverter<String, String> {

    private static final String ALGORITMO = "AES/ECB/PKCS5Padding";
    private static final HexFormat HEX = HexFormat.of().withUpperCase();

    private final SecretKeySpec clave;

    public CifradoAesConverter(CrediTrackProperties propiedades) {
        this.clave = new SecretKeySpec(propiedades.seguridad().claveAes().getBytes(StandardCharsets.UTF_8), "AES");
    }

    @Override
    public String convertToDatabaseColumn(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            Cipher cifrador = Cipher.getInstance(ALGORITMO);
            cifrador.init(Cipher.ENCRYPT_MODE, clave);
            return HEX.formatHex(cifrador.doFinal(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el dato", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String valorCifrado) {
        if (valorCifrado == null) {
            return null;
        }
        try {
            Cipher cifrador = Cipher.getInstance(ALGORITMO);
            cifrador.init(Cipher.DECRYPT_MODE, clave);
            return new String(cifrador.doFinal(HEX.parseHex(valorCifrado)), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("No se pudo descifrar el dato; revise que la clave AES sea la misma de la base", e);
        }
    }
}
