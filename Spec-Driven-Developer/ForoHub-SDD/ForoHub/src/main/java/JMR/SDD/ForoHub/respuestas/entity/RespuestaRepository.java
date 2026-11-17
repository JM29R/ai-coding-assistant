package JMR.SDD.ForoHub.respuestas.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de persistencia de las respuestas.
 */
public interface RespuestaRepository extends JpaRepository<Respuesta, Long> {

    /** Busqueda de una respuesta por id y estado (detalle, actualizar y eliminar). */
    Optional<Respuesta> findByIdAndEstado(Long id, EstadoRespuesta estado);

    /** Listado de respuestas de un topico en un estado dado, mas recientes primero. */
    List<Respuesta> findByTopicoIdAndEstadoOrderByFechaCreacionDesc(
            Long topicoId, EstadoRespuesta estado);

    /** Respuestas de un topico en un estado dado (cascade logico, R6). */
    List<Respuesta> findByTopicoIdAndEstado(Long topicoId, EstadoRespuesta estado);
}
