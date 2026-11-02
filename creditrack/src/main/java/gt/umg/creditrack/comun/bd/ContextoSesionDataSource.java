package gt.umg.creditrack.comun.bd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DelegatingDataSource;

public class ContextoSesionDataSource extends DelegatingDataSource {

    private static final String SQL_CONTEXTO =
            "SET @usuario_app = ?, @id_sesion_usuario = ?, @permitir_cambio_saldo = 0";

    public ContextoSesionDataSource(DataSource destino) {
        super(destino);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return aplicarContexto(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return aplicarContexto(super.getConnection(username, password));
    }

    private Connection aplicarContexto(Connection conexion) throws SQLException {
        ContextoSesion.Datos datos = ContextoSesion.actual().orElse(null);
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_CONTEXTO)) {
            if (datos == null || datos.username() == null) {
                sentencia.setNull(1, Types.VARCHAR);
            } else {
                sentencia.setString(1, datos.username());
            }
            if (datos == null || datos.idSesionUsuario() == null) {
                sentencia.setNull(2, Types.BIGINT);
            } else {
                sentencia.setLong(2, datos.idSesionUsuario());
            }
            sentencia.execute();
        } catch (SQLException e) {
            conexion.close();
            throw e;
        }
        return conexion;
    }
}
