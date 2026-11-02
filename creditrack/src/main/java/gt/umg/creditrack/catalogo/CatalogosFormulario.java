package gt.umg.creditrack.catalogo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import gt.umg.creditrack.comun.modelo.EstadoActivo;

@Component
public class CatalogosFormulario {

    public record GrupoMunicipios(String departamento, List<Municipio> municipios) {
    }

    private final GeneroRepository generos;
    private final DepartamentoRepository departamentos;
    private final MunicipioRepository municipios;
    private final TipoCuentaRepository tiposCuenta;

    public CatalogosFormulario(GeneroRepository generos, DepartamentoRepository departamentos,
                               MunicipioRepository municipios, TipoCuentaRepository tiposCuenta) {
        this.generos = generos;
        this.departamentos = departamentos;
        this.municipios = municipios;
        this.tiposCuenta = tiposCuenta;
    }

    @Transactional(readOnly = true)
    public List<Genero> generos() {
        return generos.findAll().stream()
                .filter(g -> g.getEstado() == EstadoActivo.ACTIVO)
                .sorted(Comparator.comparing(Genero::getNombre))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GrupoMunicipios> municipiosPorDepartamento() {
        List<GrupoMunicipios> grupos = new ArrayList<>();
        departamentos.findAll().stream()
                .sorted(Comparator.comparing(Departamento::getNombre))
                .forEach(d -> {
                    List<Municipio> lista = municipios.findByIdDepartamentoOrderByNombre(d.getIdDepartamento());
                    if (!lista.isEmpty()) {
                        grupos.add(new GrupoMunicipios(d.getNombre(), lista));
                    }
                });
        return grupos;
    }

    @Transactional(readOnly = true)
    public List<TipoCuenta> tiposCuentaActivos() {
        return tiposCuenta.findByActivoTrueOrderByNombre();
    }
}
