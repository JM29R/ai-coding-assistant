package JMR.SDD.ForoHub.respuestas.service;

/**
 * Error cuando la respuesta solicitada no existe o esta inactiva (HTTP 404).
 */
public class RespuestaNoEncontradaException extends RuntimeException {

    public RespuestaNoEncontradaException(Long id) {
        super("No se encontro una respuesta activa con id " + id);
    }
}
