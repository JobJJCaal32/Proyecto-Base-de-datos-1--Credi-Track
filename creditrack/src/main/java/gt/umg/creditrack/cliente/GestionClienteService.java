package gt.umg.creditrack.cliente;

import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gt.umg.creditrack.catalogo.MunicipioRepository;
import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.comun.modelo.EstadoActivo;
import gt.umg.creditrack.persona.Direccion;
import gt.umg.creditrack.persona.DireccionRepository;
import gt.umg.creditrack.persona.Identidad;
import gt.umg.creditrack.persona.IdentidadRepository;
import gt.umg.creditrack.usuario.EstadoUsuario;
import gt.umg.creditrack.usuario.UsuarioRepository;

@Service
public class GestionClienteService {

    public record DatosContacto(String nit, String telefono, String correo, Integer idMunicipio, String direccion) {
    }

    private final ClienteRepository clientes;
    private final IdentidadRepository identidades;
    private final DireccionRepository direcciones;
    private final MunicipioRepository municipios;
    private final UsuarioRepository usuarios;
    private final JdbcTemplate jdbc;

    public GestionClienteService(ClienteRepository clientes, IdentidadRepository identidades, DireccionRepository direcciones,
                                 MunicipioRepository municipios, UsuarioRepository usuarios, JdbcTemplate jdbc) {
        this.clientes = clientes;
        this.identidades = identidades;
        this.direcciones = direcciones;
        this.municipios = municipios;
        this.usuarios = usuarios;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public DatosContacto datos(String codigo) {
        Cliente cliente = cliente(codigo);
        Identidad identidad = identidades.findById(cliente.getIdIdentidad()).orElseThrow();
        Direccion direccion = direcciones.findById(identidad.getIdDireccion()).orElseThrow();
        String correo = usuarios.findByIdCliente(cliente.getIdCliente()).map(u -> u.getCorreo()).orElse(null);
        return new DatosContacto(identidad.getNit(), identidad.getTelefono(), correo, direccion.getIdMunicipio(),
                direccion.getDescripcionDireccion());
    }

    @Transactional
    public void actualizar(String codigo, DatosContacto datos) {
        Cliente cliente = cliente(codigo);
        if (cliente.getEstado() != EstadoActivo.ACTIVO) {
            throw new ReglaNegocioException("El cliente está dado de baja; no se pueden cambiar sus datos.");
        }
        String nit = vacioANulo(datos.nit());
        if (nit != null && !nit.toUpperCase().matches("^(CF|\\d{1,12}-?[\\dK])$")) {
            throw new ReglaNegocioException("Escriba el NIT como 1234567-8 o CF.");
        }
        String telefono = vacioANulo(datos.telefono());
        if (telefono != null && !telefono.matches("^\\d{4}-?\\d{4}$")) {
            throw new ReglaNegocioException("El teléfono tiene 8 dígitos, por ejemplo 5512-3408.");
        }
        String direccionTexto = vacioANulo(datos.direccion());
        if (direccionTexto == null || direccionTexto.length() > 255) {
            throw new ReglaNegocioException("Escriba la dirección (máximo 255 caracteres).");
        }
        if (datos.idMunicipio() == null || municipios.findById(datos.idMunicipio()).isEmpty()) {
            throw new ReglaNegocioException("Seleccione un municipio válido.");
        }
        Identidad identidad = identidades.findById(cliente.getIdIdentidad()).orElseThrow();
        identidad.setNit(nit == null ? null : nit.toUpperCase());
        identidad.setTelefono(telefono == null ? null : telefono.replaceAll("\\D", "").replaceFirst("(\\d{4})(\\d{4})", "$1-$2"));
        Direccion direccion = direcciones.findById(identidad.getIdDireccion()).orElseThrow();
        direccion.setIdMunicipio(datos.idMunicipio());
        direccion.setDescripcionDireccion(direccionTexto.replaceAll("\\s+", " "));
        String correo = vacioANulo(datos.correo());
        usuarios.findByIdCliente(cliente.getIdCliente()).ifPresent(usuario -> {
            if (correo == null || !correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || correo.length() > 100) {
                throw new ReglaNegocioException("Escriba un correo válido.");
            }
            String nuevo = correo.toLowerCase();
            if (!nuevo.equals(usuario.getCorreo()) && usuarios.existsByCorreo(nuevo)) {
                throw new ReglaNegocioException("Ese correo ya está registrado en otro usuario.");
            }
            usuario.setCorreo(nuevo);
        });
    }

    @Transactional
    public void darDeBaja(String codigo) {
        Cliente cliente = cliente(codigo);
        if (cliente.getEstado() != EstadoActivo.ACTIVO) {
            throw new ReglaNegocioException("El cliente ya está dado de baja.");
        }
        Integer abiertas = jdbc.queryForObject("""
                SELECT COUNT(*) FROM cuenta c JOIN estado_cuenta ec ON ec.id_estado_cuenta = c.id_estado_cuenta
                 WHERE c.id_cliente = ? AND ec.nombre <> 'CERRADA'
                """, Integer.class, cliente.getIdCliente());
        if (abiertas != null && abiertas > 0) {
            throw new ReglaNegocioException("Antes de dar de baja al cliente cierre sus " + abiertas + " cuenta(s).");
        }
        cliente.setEstado(EstadoActivo.INACTIVO);
        cliente.setFechaBaja(LocalDate.now(ZoneOffset.UTC));
        usuarios.findByIdCliente(cliente.getIdCliente()).ifPresent(u -> u.setEstado(EstadoUsuario.INACTIVO));
    }

    private Cliente cliente(String codigo) {
        return clientes.findByCodigoCliente(codigo == null ? "" : codigo.trim().toUpperCase())
                .orElseThrow(() -> new ReglaNegocioException("El cliente no existe."));
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
