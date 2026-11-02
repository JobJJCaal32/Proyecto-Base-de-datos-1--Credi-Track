package gt.umg.creditrack.persona;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

public class DatosPersonaForm {

    public static final String PATRON_NOMBRE = "^[\\p{L}]+([ '\\-][\\p{L}]+)*$";

    @NotBlank(message = "Escriba el primer nombre.")
    @Size(max = 50, message = "Máximo 50 caracteres.")
    @Pattern(regexp = PATRON_NOMBRE, message = "Use solo letras.")
    private String primerNombre;

    @Size(max = 50, message = "Máximo 50 caracteres.")
    @Pattern(regexp = PATRON_NOMBRE, message = "Use solo letras.")
    private String segundoNombre;

    @Size(max = 50, message = "Máximo 50 caracteres.")
    @Pattern(regexp = PATRON_NOMBRE, message = "Use solo letras.")
    private String tercerNombre;

    @NotBlank(message = "Escriba el primer apellido.")
    @Size(max = 50, message = "Máximo 50 caracteres.")
    @Pattern(regexp = PATRON_NOMBRE, message = "Use solo letras.")
    private String primerApellido;

    @Size(max = 50, message = "Máximo 50 caracteres.")
    @Pattern(regexp = PATRON_NOMBRE, message = "Use solo letras.")
    private String segundoApellido;

    @NotBlank(message = "Escriba el DPI.")
    @Pattern(regexp = "^\\d{13}$", message = "El DPI tiene 13 dígitos, sin espacios.")
    private String dpi;

    @Size(max = 20, message = "Máximo 20 caracteres.")
    @Pattern(regexp = "^(CF|\\d{1,12}-?[\\dK])$", message = "Escriba el NIT como 1234567-8 o CF.")
    private String nit;

    @NotNull(message = "Indique la fecha de nacimiento.")
    @Past(message = "La fecha de nacimiento debe ser anterior a hoy.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaNacimiento;

    @NotNull(message = "Seleccione el género.")
    private Integer idGenero;

    @Pattern(regexp = "^\\d{4}-?\\d{4}$", message = "Escriba 8 dígitos, por ejemplo 5512-3408.")
    private String telefono;

    @NotBlank(message = "Escriba el correo electrónico.")
    @Email(message = "Escriba un correo válido.")
    @Size(max = 100, message = "Máximo 100 caracteres.")
    private String correo;

    @NotNull(message = "Seleccione el municipio.")
    private Integer idMunicipio;

    @NotBlank(message = "Escriba la dirección.")
    @Size(max = 255, message = "Máximo 255 caracteres.")
    private String direccion;

    public String getPrimerNombre() {
        return primerNombre;
    }

    public void setPrimerNombre(String primerNombre) {
        this.primerNombre = primerNombre;
    }

    public String getSegundoNombre() {
        return segundoNombre;
    }

    public void setSegundoNombre(String segundoNombre) {
        this.segundoNombre = segundoNombre;
    }

    public String getTercerNombre() {
        return tercerNombre;
    }

    public void setTercerNombre(String tercerNombre) {
        this.tercerNombre = tercerNombre;
    }

    public String getPrimerApellido() {
        return primerApellido;
    }

    public void setPrimerApellido(String primerApellido) {
        this.primerApellido = primerApellido;
    }

    public String getSegundoApellido() {
        return segundoApellido;
    }

    public void setSegundoApellido(String segundoApellido) {
        this.segundoApellido = segundoApellido;
    }

    public String getDpi() {
        return dpi;
    }

    public void setDpi(String dpi) {
        this.dpi = dpi == null ? null : dpi.replaceAll("[\\s-]", "");
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit == null ? null : nit.toUpperCase();
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public Integer getIdGenero() {
        return idGenero;
    }

    public void setIdGenero(Integer idGenero) {
        this.idGenero = idGenero;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo == null ? null : correo.toLowerCase();
    }

    public Integer getIdMunicipio() {
        return idMunicipio;
    }

    public void setIdMunicipio(Integer idMunicipio) {
        this.idMunicipio = idMunicipio;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
}
