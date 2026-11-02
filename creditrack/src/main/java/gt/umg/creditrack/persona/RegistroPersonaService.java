package gt.umg.creditrack.persona;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import gt.umg.creditrack.catalogo.GeneroRepository;
import gt.umg.creditrack.catalogo.MunicipioRepository;
import gt.umg.creditrack.cliente.ClienteRepository;
import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.config.CrediTrackProperties;
import gt.umg.creditrack.usuario.UsuarioRepository;

@Service
public class RegistroPersonaService {

    private static final int EDAD_MINIMA = 18;

    private final DireccionRepository direcciones;
    private final IdentidadRepository identidades;
    private final ClienteRepository clientes;
    private final UsuarioRepository usuarios;
    private final GeneroRepository generos;
    private final MunicipioRepository municipios;
    private final CrediTrackProperties propiedades;

    public RegistroPersonaService(DireccionRepository direcciones, IdentidadRepository identidades, ClienteRepository clientes,
                                  UsuarioRepository usuarios, GeneroRepository generos,
                                  MunicipioRepository municipios, CrediTrackProperties propiedades) {
        this.direcciones = direcciones;
        this.identidades = identidades;
        this.clientes = clientes;
        this.usuarios = usuarios;
        this.generos = generos;
        this.municipios = municipios;
        this.propiedades = propiedades;
    }

    @Transactional(readOnly = true)
    public void validar(DatosPersonaForm form) {
        LocalDate hoy = LocalDate.now(propiedades.zona());
        if (form.getFechaNacimiento().isAfter(hoy.minusYears(EDAD_MINIMA))) {
            throw new ReglaNegocioException("La persona debe ser mayor de " + EDAD_MINIMA + " años.");
        }
        if (generos.findById(form.getIdGenero()).isEmpty()) {
            throw new ReglaNegocioException("Seleccione un género válido.");
        }
        if (municipios.findById(form.getIdMunicipio()).isEmpty()) {
            throw new ReglaNegocioException("Seleccione un municipio válido.");
        }
        identidades.findByDpi(form.getDpi()).ifPresent(existente -> {
            String mensaje = clientes.findByIdIdentidad(existente.getIdIdentidad())
                    .map(c -> "Ya existe un cliente con ese DPI: " + c.getCodigoCliente() + ".")
                    .orElse("Ese DPI ya está registrado en el banco a nombre de otra persona.");
            throw new ReglaNegocioException(mensaje);
        });
        if (usuarios.existsByCorreo(form.getCorreo())) {
            throw new ReglaNegocioException("Ese correo ya está registrado en otro usuario.");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Identidad crear(DatosPersonaForm form) {
        Direccion direccion = new Direccion();
        direccion.setIdMunicipio(form.getIdMunicipio());
        direccion.setDescripcionDireccion(limpiar(form.getDireccion()));
        direcciones.save(direccion);

        Identidad identidad = new Identidad();
        identidad.setDpi(form.getDpi());
        identidad.setNit(form.getNit());
        identidad.setPrimerNombre(limpiar(form.getPrimerNombre()));
        identidad.setSegundoNombre(limpiar(form.getSegundoNombre()));
        identidad.setTercerNombre(limpiar(form.getTercerNombre()));
        identidad.setPrimerApellido(limpiar(form.getPrimerApellido()));
        identidad.setSegundoApellido(limpiar(form.getSegundoApellido()));
        identidad.setFechaNacimiento(form.getFechaNacimiento());
        identidad.setIdGenero(form.getIdGenero());
        identidad.setTelefono(telefonoNormalizado(form.getTelefono()));
        identidad.setIdDireccion(direccion.getIdDireccion());
        return identidades.save(identidad);
    }

    private static String telefonoNormalizado(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            return null;
        }
        String digitos = telefono.replaceAll("\\D", "");
        return digitos.substring(0, 4) + "-" + digitos.substring(4);
    }

    private static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim().replaceAll("\\s+", " ");
        return limpio.isEmpty() ? null : limpio;
    }
}
