package JMR.SDD.ForoHub.respuestas.controller;

/**
 * Datos de entrada para crear y actualizar una respuesta.
 * No incluye id, estado ni fecha: los gestiona el backend.
 */
public record RespuestaDTORequest(
        String mensaje,
        String autor,
        Long topicoId
) {
}
