package JMR.SDD.ForoHub.Topicos.controller;

import JMR.SDD.ForoHub.Topicos.entity.EstadoTopico;

import java.time.LocalDateTime;

/**
 * Datos de salida de todo topico devuelto por el backend.
 */
public record TopicoDTOResponse(
        Long id,
        String titulo,
        String mensaje,
        LocalDateTime fechaCreacion,
        EstadoTopico estado,
        String autor
) {
}
