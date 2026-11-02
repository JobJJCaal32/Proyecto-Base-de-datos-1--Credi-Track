package gt.umg.creditrack.config;

import java.math.BigDecimal;
import java.time.ZoneId;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "creditrack")
public record CrediTrackProperties(
        @NotBlank String zonaHoraria,
        @Valid @NotNull Seguridad seguridad,
        @Valid @NotNull Sesion sesion,
        @Valid @NotNull Caja caja) {

    public record Seguridad(
            @NotBlank @Size(min = 16, max = 16) String claveAes,
            @Min(1) @Max(10) int maxIntentos) {
    }

    public record Sesion(
            @NotBlank String nombreCookie,
            @Min(1) @Max(240) int minutosInactividad,
            boolean cookieSegura) {
    }

    public record Caja(
            @NotNull @DecimalMin("0.01") BigDecimal limiteRetiroSinAutorizacion) {
    }

    public ZoneId zona() {
        return ZoneId.of(zonaHoraria);
    }
}
