package gt.umg.creditrack.comun.bd;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import jakarta.persistence.EntityManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import gt.umg.creditrack.cliente.Cliente;
import gt.umg.creditrack.cliente.ClienteRepository;
import gt.umg.creditrack.config.CrediTrackProperties;
import gt.umg.creditrack.persona.Identidad;
import gt.umg.creditrack.persona.IdentidadRepository;

@Component
@Profile("dev")
public class VerificacionConexion implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(VerificacionConexion.class);
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CLIENTE_PRUEBA = "CLI-000001";

    private final JdbcTemplate jdbc;
    private final CrediTrackProperties propiedades;
    private final EntityManagerFactory entityManagerFactory;
    private final ClienteRepository clientes;
    private final IdentidadRepository identidades;

    public VerificacionConexion(JdbcTemplate jdbc, CrediTrackProperties propiedades, EntityManagerFactory entityManagerFactory,
                                ClienteRepository clientes, IdentidadRepository identidades) {
        this.jdbc = jdbc;
        this.propiedades = propiedades;
        this.entityManagerFactory = entityManagerFactory;
        this.clientes = clientes;
        this.identidades = identidades;
    }

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        Map<String, Object> info = jdbc.queryForMap("""
                SELECT DATABASE() AS base,
                       CURRENT_USER() AS usuario,
                       VERSION() AS version,
                       @@session.time_zone AS zona,
                       DATE_FORMAT(UTC_TIMESTAMP(), '%Y-%m-%d %H:%i:%s') AS hora_utc,
                       (SELECT COUNT(*) FROM cuenta) AS cuentas
                """);
        String horaLocal = ZonedDateTime.now(propiedades.zona()).format(FORMATO);
        log.info("Conexión correcta | base: {} | usuario: {} | MySQL {} | zona de la sesión: {}",
                info.get("base"), info.get("usuario"), info.get("version"), info.get("zona"));
        log.info("Hora UTC en MySQL: {} | Hora en {}: {} | Cuentas registradas: {}",
                info.get("hora_utc"), propiedades.zonaHoraria(), horaLocal, info.get("cuentas"));

        int entidades = entityManagerFactory.getMetamodel().getEntities().size();
        log.info("Mapeo validado contra la base: {} entidades", entidades);

        verificarCifrado();
    }

    private void verificarCifrado() {
        Cliente cliente = clientes.findByCodigoCliente(CLIENTE_PRUEBA).orElse(null);
        if (cliente == null) {
            log.warn("Cifrado AES: no existe el cliente {} para probar", CLIENTE_PRUEBA);
            return;
        }
        Identidad identidad = identidades.findById(cliente.getIdIdentidad()).orElseThrow();
        String dpiMysql = jdbc.queryForObject(
                "SELECT CAST(AES_DECRYPT(UNHEX(dpi), ?) AS CHAR) FROM identidad WHERE id_identidad = ?",
                String.class, propiedades.seguridad().claveAes(), identidad.getIdIdentidad());
        boolean coinciden = identidad.getDpi().equals(dpiMysql);
        boolean busquedaPorDpi = identidades.findByDpi(identidad.getDpi()).isPresent();
        String dpi = identidad.getDpi();
        String dpiOculto = "*".repeat(Math.max(0, dpi.length() - 4)) + dpi.substring(Math.max(0, dpi.length() - 4));
        if (coinciden && busquedaPorDpi) {
            log.info("Cifrado AES correcto: Java y MySQL descifran igual el DPI de {} ({}) y la búsqueda por DPI funciona",
                    CLIENTE_PRUEBA, dpiOculto);
        } else {
            log.error("Cifrado AES con problemas | coinciden: {} | búsqueda por DPI: {}", coinciden, busquedaPorDpi);
        }
    }
}
