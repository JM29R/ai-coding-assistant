package JMR.SDD.ForoHub.respuestas.controller;

import JMR.SDD.ForoHub.respuestas.entity.EstadoRespuesta;

import java.time.LocalDateTime;

/**
 * Datos de salida de toda respuesta devuelta por el backend.
 */
public record RespuestaDTOResponse(
        Long id,
        String mensaje,
        LocalDateTime fechaCreacion,
        EstadoRespuesta estado,
        String autor,
        Long topicoId
) {
}
