package gt.umg.creditrack.sucursal;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gt.umg.creditrack.catalogo.MunicipioRepository;
import gt.umg.creditrack.comun.error.ReglaNegocioException;
import gt.umg.creditrack.comun.modelo.EstadoActivo;

@Service
public class SucursalService {

    public record FilaSucursal(Integer id, String codigo, String nombre, String municipio, String direccion, String telefono,
                               String responsable, Integer idResponsable, String estado, LocalDate fechaApertura, long empleados) {
    }

    private final SucursalRepository sucursales;
    private final EmpleadoRepository empleados;
    private final MunicipioRepository municipios;
    private final JdbcTemplate jdbc;

    public SucursalService(SucursalRepository sucursales, EmpleadoRepository empleados, MunicipioRepository municipios,
                           JdbcTemplate jdbc) {
        this.sucursales = sucursales;
        this.empleados = empleados;
        this.municipios = municipios;
        this.jdbc = jdbc;
    }

    public List<FilaSucursal> listar() {
        return jdbc.query("""
                SELECT s.id_sucursal, s.codigo_sucursal, s.nombre, m.nombre AS municipio, s.direccion, s.telefono,
                       CONCAT_WS(' ', i.primer_nombre, i.primer_apellido) AS responsable, s.id_empleado_responsable,
                       s.estado, s.fecha_apertura,
                       (SELECT COUNT(*) FROM empleado x WHERE x.id_sucursal = s.id_sucursal AND x.estado = 'ACTIVO') AS empleados
                  FROM sucursal s
                  JOIN municipio m ON m.id_municipio = s.id_municipio
                  LEFT JOIN empleado e ON e.id_empleado = s.id_empleado_responsable
                  LEFT JOIN identidad i ON i.id_identidad = e.id_identidad
                 ORDER BY s.codigo_sucursal
                """, (f, n) -> new FilaSucursal(f.getInt("id_sucursal"), f.getString("codigo_sucursal"), f.getString("nombre"),
                f.getString("municipio"), f.getString("direccion"), f.getString("telefono"), f.getString("responsable"),
                (Integer) f.getObject("id_empleado_responsable"), f.getString("estado"), f.getDate("fecha_apertura").toLocalDate(),
                f.getLong("empleados")));
    }

    public List<Empleado> empleadosActivos(Integer idSucursal) {
        return empleados.findAll().stream()
                .filter(e -> e.getIdSucursal().equals(idSucursal) && e.getEstado() == EstadoActivo.ACTIVO)
                .toList();
    }

    @Transactional
    public void crear(String codigo, String nombre, Integer idMunicipio, String direccion, String telefono, LocalDate fechaApertura) {
        String cod = codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
        if (!cod.matches("^[A-Z]{2}-\\d{3}$")) {
            throw new ReglaNegocioException("El código tiene la forma AG-002.");
        }
        if (sucursales.findByCodigoSucursal(cod).isPresent()) {
            throw new ReglaNegocioException("Ya existe una sucursal con el código " + cod + ".");
        }
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > 80) {
            throw new ReglaNegocioException("Escriba el nombre de la sucursal (máximo 80 caracteres).");
        }
        if (idMunicipio == null || municipios.findById(idMunicipio).isEmpty()) {
            throw new ReglaNegocioException("Seleccione un municipio válido.");
        }
        if (direccion == null || direccion.isBlank() || direccion.trim().length() > 200) {
            throw new ReglaNegocioException("Escriba la dirección (máximo 200 caracteres).");
        }
        String tel = telefono == null || telefono.isBlank() ? null : telefono.trim();
        if (tel != null && !tel.matches("^\\d{4}-?\\d{4}$")) {
            throw new ReglaNegocioException("El teléfono tiene 8 dígitos, por ejemplo 7951-0002.");
        }
        Sucursal sucursal = new Sucursal();
        sucursal.setCodigoSucursal(cod);
        sucursal.setNombre(nombre.trim());
        sucursal.setIdMunicipio(idMunicipio);
        sucursal.setDireccion(direccion.trim());
        sucursal.setTelefono(tel == null ? null : tel.replaceAll("\\D", "").replaceFirst("(\\d{4})(\\d{4})", "$1-$2"));
        sucursal.setEstado(EstadoSucursal.ACTIVA);
        sucursal.setFechaApertura(fechaApertura == null ? LocalDate.now() : fechaApertura);
        sucursales.save(sucursal);
    }

    @Transactional
    public void asignarResponsable(Integer idSucursal, Integer idEmpleado) {
        Sucursal sucursal = sucursales.findById(idSucursal == null ? -1 : idSucursal)
                .orElseThrow(() -> new ReglaNegocioException("La sucursal no existe."));
        Empleado empleado = empleados.findById(idEmpleado == null ? -1 : idEmpleado)
                .filter(e -> e.getIdSucursal().equals(sucursal.getIdSucursal()) && e.getEstado() == EstadoActivo.ACTIVO)
                .orElseThrow(() -> new ReglaNegocioException("El responsable debe ser un empleado activo de esa sucursal."));
        sucursal.setIdEmpleadoResponsable(empleado.getIdEmpleado());
    }

    @Transactional
    public void cambiarEstado(Integer idSucursal) {
        Sucursal sucursal = sucursales.findById(idSucursal == null ? -1 : idSucursal)
                .orElseThrow(() -> new ReglaNegocioException("La sucursal no existe."));
        if (sucursal.getEstado() == EstadoSucursal.ACTIVA) {
            long activos = empleadosActivos(sucursal.getIdSucursal()).size();
            if (activos > 0) {
                throw new ReglaNegocioException("La sucursal tiene " + activos + " empleado(s) activo(s); trasládelos antes de cerrarla.");
            }
            sucursal.setEstado(EstadoSucursal.INACTIVA);
        } else {
            sucursal.setEstado(EstadoSucursal.ACTIVA);
        }
    }
}
