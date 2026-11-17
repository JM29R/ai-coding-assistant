package JMR.SDD.ForoHub.respuestas.service;

/**
 * Error de validacion de negocio del modulo de respuestas (HTTP 400).
 */
public class ValidacionRespuestaException extends RuntimeException {

    public ValidacionRespuestaException(String mensaje) {
        super(mensaje);
    }
}
