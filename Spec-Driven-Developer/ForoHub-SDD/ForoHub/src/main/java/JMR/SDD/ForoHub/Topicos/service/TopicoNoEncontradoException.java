package JMR.SDD.ForoHub.Topicos.service;

/**
 * Error cuando el topico solicitado no existe o esta inactivo (HTTP 404).
 */
public class TopicoNoEncontradoException extends RuntimeException {

    public TopicoNoEncontradoException(Long id) {
        super("No se encontro un topico activo con id " + id);
    }
}
