package JMR.SDD.ForoHub.Topicos.service;

/**
 * Error de validacion de negocio del modulo de topicos (HTTP 400).
 */
public class ValidacionTopicoException extends RuntimeException {

    public ValidacionTopicoException(String mensaje) {
        super(mensaje);
    }
}
