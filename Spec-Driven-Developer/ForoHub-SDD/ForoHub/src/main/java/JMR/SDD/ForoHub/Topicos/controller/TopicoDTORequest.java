package JMR.SDD.ForoHub.Topicos.controller;

/**
 * Datos de entrada para crear y actualizar un topico.
 * No incluye id: lo genera la base de datos.
 */
public record TopicoDTORequest(
        String titulo,
        String mensaje,
        String autor
) {
}
