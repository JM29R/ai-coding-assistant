package JMR.SDD.ForoHub.Topicos.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de persistencia de los topicos.
 */
public interface TopicoRepository extends JpaRepository<Topico, Long> {

    /** Listado completo de topicos en un estado dado, mas recientes primero. */
    List<Topico> findByEstadoOrderByFechaCreacionDesc(EstadoTopico estado);

    /** Busqueda de un topico por id y estado (detalle, actualizar y eliminar). */
    Optional<Topico> findByIdAndEstado(Long id, EstadoTopico estado);

    /** Verificacion de duplicados al crear (R2), sin distinguir mayusculas. */
    boolean existsByTituloIgnoreCaseAndMensajeIgnoreCaseAndEstado(
            String titulo, String mensaje, EstadoTopico estado);

    /** Verificacion de duplicados al actualizar (R2), excluyendo el propio topico. */
    boolean existsByTituloIgnoreCaseAndMensajeIgnoreCaseAndEstadoAndIdNot(
            String titulo, String mensaje, EstadoTopico estado, Long id);
}
