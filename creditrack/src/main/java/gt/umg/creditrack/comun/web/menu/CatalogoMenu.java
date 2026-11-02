package gt.umg.creditrack.comun.web.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import gt.umg.creditrack.comun.modelo.TipoUsuario;
import gt.umg.creditrack.seguridad.UsuarioActual;

@Component
public class CatalogoMenu {

    public static final String INICIO_CLIENTE = "/banca";
    public static final String INICIO_EMPLEADO = "/empleado";
    public static final String CAMBIAR_CONTRASENA = "/cambiar-contrasena";

    private static final TipoUsuario C = TipoUsuario.CLIENTE;
    private static final TipoUsuario E = TipoUsuario.EMPLEADO;

    private final List<SeccionMenu> secciones = List.of(
            seccion("Banca virtual",
                    new OpcionMenu(C, "Inicio", INICIO_CLIENTE, null, "Resumen de su banca virtual", true),
                    new OpcionMenu(C, "Mis cuentas", "/banca/cuentas", "CONSULTAR_CUENTAS", "Saldos y alias de sus cuentas", true),
                    new OpcionMenu(C, "Transferir", "/banca/transferir", "TRANSFERIR", "Enviar dinero a otra cuenta", true),
                    new OpcionMenu(C, "Movimientos", "/banca/movimientos", "CONSULTAR_MOVIMIENTOS", "Historial por mes o por tipo", true),
                    new OpcionMenu(C, "Estado de cuenta", "/banca/estado-cuenta", "GENERAR_ESTADO_CUENTA", "Resumen de un periodo", true)),
            seccion("Seguridad",
                    new OpcionMenu(C, "Clave de operación", "/banca/clave-operacion", "CONFIGURAR_CLAVE_OPERACION", "Clave para autorizar transferencias", true),
                    new OpcionMenu(C, "Mis accesos", "/banca/accesos", "VER_ACCESOS", "Sus últimos inicios de sesión", true),
                    new OpcionMenu(C, "Cambiar contraseña", CAMBIAR_CONTRASENA, "CAMBIAR_CONTRASENA", "Actualizar su contraseña", true)),
            seccion("General",
                    new OpcionMenu(E, "Inicio", INICIO_EMPLEADO, null, "Resumen de su trabajo", true)),
            seccion("Clientes",
                    new OpcionMenu(E, "Registrar cliente", "/empleado/clientes/nuevo", "REGISTRAR_CLIENTE", "Datos personales, cuenta y usuario de banca virtual", true),
                    new OpcionMenu(E, "Buscar cliente", "/empleado/clientes", "BUSCAR_CLIENTE", "Por DPI o código de cliente", true),
                    new OpcionMenu(E, "Abrir cuenta", "/empleado/cuentas/nueva", "ABRIR_CUENTA", "Cuenta nueva con depósito de apertura", true),
                    new OpcionMenu(E, "Estado de cuentas", "/empleado/cuentas/estado", "CAMBIAR_ESTADO_CUENTA", "Bloquear, desbloquear o cerrar cuentas", true),
                    new OpcionMenu(E, "Cuentas de clientes", "/empleado/cuentas", "CONSULTAR_CUENTAS", "Saldos de las cuentas de un cliente", true)),
            seccion("Caja",
                    new OpcionMenu(E, "Mi caja", "/empleado/caja", "ABRIR_CAJA", "Abrir y cerrar su caja", true),
                    new OpcionMenu(E, "Depósito", "/empleado/caja/deposito", "REALIZAR_DEPOSITO", "Recibir efectivo en una cuenta", true),
                    new OpcionMenu(E, "Retiro", "/empleado/caja/retiro", "REALIZAR_RETIRO", "Entregar efectivo de una cuenta", true),
                    new OpcionMenu(E, "Transferencia en ventanilla", "/empleado/caja/transferencia", "TRANSFERIR_VENTANILLA", "Transferir entre cuentas en ventanilla", true)),
            seccion("Supervisión",
                    new OpcionMenu(E, "Arqueo de cajas", "/empleado/supervision/arqueos", "ARQUEAR_CAJA", "Revisar el cierre de las cajas", true),
                    new OpcionMenu(E, "Autorizaciones", "/empleado/supervision/autorizaciones", "AUTORIZAR_OPERACION", "Operaciones sobre el límite", true),
                    new OpcionMenu(E, "Reversos", "/empleado/supervision/reversos", "REVERSAR_TRANSACCION", "Anular transferencias aplicadas", true),
                    new OpcionMenu(E, "Alertas", "/empleado/supervision/alertas", "GESTIONAR_ALERTAS", "Revisar y resolver alertas", true)),
            seccion("Administración",
                    new OpcionMenu(E, "Registrar empleado", "/empleado/admin/empleados/nuevo", "REGISTRAR_EMPLEADO", "Empleado nuevo con su usuario", true),
                    new OpcionMenu(E, "Empleados", "/empleado/admin/empleados", "GESTIONAR_EMPLEADOS", "Puesto, sucursal y bajas", true),
                    new OpcionMenu(E, "Usuarios", "/empleado/admin/usuarios", "DESBLOQUEAR_USUARIO", "Desbloquear y resetear contraseñas", true),
                    new OpcionMenu(E, "Permisos por rol", "/empleado/admin/permisos", "ASIGNAR_PERMISOS", "Qué puede hacer cada rol", true),
                    new OpcionMenu(E, "Sucursales", "/empleado/admin/sucursales", "GESTIONAR_SUCURSALES", "Agencias y sus responsables", true)),
            seccion("Consultas y auditoría",
                    new OpcionMenu(E, "Movimientos", "/empleado/consultas/movimientos", "CONSULTAR_MOVIMIENTOS", "Por mes, tipo o cliente", true),
                    new OpcionMenu(E, "Operaciones por sucursal", "/empleado/consultas/operaciones", "CONSULTAR_REPORTES", "Por sucursal o cajero", true),
                    new OpcionMenu(E, "Bitácora", "/empleado/auditoria/bitacora", "CONSULTAR_BITACORA", "Quién cambió qué y cuándo", true),
                    new OpcionMenu(E, "Intentos de acceso", "/empleado/auditoria/intentos", "CONSULTAR_INTENTOS_ACCESO", "Accesos fallidos por usuario o IP", true),
                    new OpcionMenu(E, "Consultas de clientes", "/empleado/auditoria/consultas", "CONSULTAR_BITACORA", "Quién consultó qué cliente", true),
                    new OpcionMenu(E, "Cajas con diferencias", "/empleado/auditoria/cajas", "CONSULTAR_REPORTES", "Faltantes y sobrantes al cerrar caja", true),
                    new OpcionMenu(E, "Verificación de consistencia", "/empleado/auditoria/consistencia", "CONSULTAR_REPORTES", "Saldos, movimientos y cajas cuadran", true)),
            seccion("Mi cuenta",
                    new OpcionMenu(E, "Cambiar contraseña", CAMBIAR_CONTRASENA, "CAMBIAR_CONTRASENA", "Actualizar su contraseña", true)));

    public List<SeccionMenu> para(UsuarioActual usuario) {
        if (usuario == null) {
            return List.of();
        }
        List<SeccionMenu> visibles = new ArrayList<>();
        for (SeccionMenu seccion : secciones) {
            List<OpcionMenu> opciones = seccion.opciones().stream()
                    .filter(o -> puedeVer(usuario, o))
                    .toList();
            if (!opciones.isEmpty()) {
                visibles.add(new SeccionMenu(seccion.titulo(), opciones));
            }
        }
        return visibles;
    }

    public List<OpcionMenu> accesosRapidos(UsuarioActual usuario) {
        return para(usuario).stream()
                .flatMap(s -> s.opciones().stream())
                .filter(o -> !o.esInicio() && !CAMBIAR_CONTRASENA.equals(o.ruta()))
                .toList();
    }

    public Optional<OpcionMenu> buscar(String ruta) {
        return opciones().stream().filter(o -> o.ruta().equals(ruta)).findFirst();
    }

    public List<OpcionMenu> protegidasDeMasEspecificaAMenos() {
        return opciones().stream()
                .filter(OpcionMenu::protegidaPorPermiso)
                .sorted(Comparator.comparingInt((OpcionMenu o) -> o.ruta().length()).reversed())
                .toList();
    }

    public static String inicioDe(UsuarioActual usuario) {
        return usuario.esCliente() ? INICIO_CLIENTE : INICIO_EMPLEADO;
    }

    private List<OpcionMenu> opciones() {
        return secciones.stream().flatMap(s -> s.opciones().stream()).toList();
    }

    private static boolean puedeVer(UsuarioActual usuario, OpcionMenu opcion) {
        return opcion.tipo() == usuario.tipo() && (opcion.permiso() == null || usuario.tienePermiso(opcion.permiso()));
    }

    private static SeccionMenu seccion(String titulo, OpcionMenu... opciones) {
        return new SeccionMenu(titulo, List.of(opciones));
    }
}
