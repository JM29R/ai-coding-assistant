package JMR.SDD.TicketMaster.evento.controller;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import JMR.SDD.TicketMaster.evento.exception.EventoBusinessException;
import JMR.SDD.TicketMaster.evento.exception.EventoNotFoundException;

@RestControllerAdvice
public class EventoExceptionHandler {

    @ExceptionHandler(EventoNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EventoNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(EventoBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(EventoBusinessException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String mensaje = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ErrorResponse(mensaje));
    }

    public record ErrorResponse(String mensaje) {
    }
}
