package gt.umg.creditrack.usuario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByIdCliente(Integer idCliente);

    Optional<Usuario> findByIdEmpleado(Integer idEmpleado);

    boolean existsByUsername(String username);

    boolean existsByCorreo(String correo);
}
