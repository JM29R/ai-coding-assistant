package JMR.SDD.ForoHub.Topicos.controller;

import JMR.SDD.ForoHub.Topicos.service.TopicoNoEncontradoException;
import JMR.SDD.ForoHub.Topicos.service.ValidacionTopicoException;
import JMR.SDD.ForoHub.respuestas.service.RespuestaNoEncontradaException;
import JMR.SDD.ForoHub.respuestas.service.ValidacionRespuestaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de dominio a errores JSON {"mensaje": "..."}.
 */
@RestControllerAdvice
public class ManejadorExcepciones {

    @ExceptionHandler(ValidacionTopicoException.class)
    public ResponseEntity<Error> manejarValidacion(ValidacionTopicoException excepcion) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new Error(excepcion.getMessage()));
    }

    @ExceptionHandler(TopicoNoEncontradoException.class)
    public ResponseEntity<Error> manejarNoEncontrado(TopicoNoEncontradoException excepcion) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new Error(excepcion.getMessage()));
    }

    @ExceptionHandler(ValidacionRespuestaException.class)
    public ResponseEntity<Error> manejarValidacionRespuesta(ValidacionRespuestaException excepcion) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new Error(excepcion.getMessage()));
    }

    @ExceptionHandler(RespuestaNoEncontradaException.class)
    public ResponseEntity<Error> manejarRespuestaNoEncontrada(RespuestaNoEncontradaException excepcion) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new Error(excepcion.getMessage()));
    }

    /** Cuerpo de error del contrato de la API. */
    public record Error(String mensaje) {
    }
}
