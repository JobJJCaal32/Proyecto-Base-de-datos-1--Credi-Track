package gt.umg.creditrack.sucursal;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import gt.umg.creditrack.persona.DatosPersonaForm;

public class RegistroEmpleadoForm extends DatosPersonaForm {

    @NotNull(message = "Seleccione la sucursal.")
    private Integer idSucursal;

    @NotBlank(message = "Escriba el puesto.")
    @Size(max = 50, message = "Máximo 50 caracteres.")
    private String puesto;

    @NotNull(message = "Indique la fecha de ingreso.")
    @PastOrPresent(message = "La fecha de ingreso no puede ser futura.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaIngreso;

    @NotNull(message = "Seleccione el rol.")
    private Integer idRol;

    public Integer getIdSucursal() {
        return idSucursal;
    }

    public void setIdSucursal(Integer idSucursal) {
        this.idSucursal = idSucursal;
    }

    public String getPuesto() {
        return puesto;
    }

    public void setPuesto(String puesto) {
        this.puesto = puesto;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public Integer getIdRol() {
        return idRol;
    }

    public void setIdRol(Integer idRol) {
        this.idRol = idRol;
    }
}
