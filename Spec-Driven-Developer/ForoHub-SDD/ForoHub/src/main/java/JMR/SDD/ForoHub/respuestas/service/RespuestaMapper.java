package JMR.SDD.ForoHub.respuestas.service;

import JMR.SDD.ForoHub.respuestas.controller.RespuestaDTORequest;
import JMR.SDD.ForoHub.respuestas.controller.RespuestaDTOResponse;
import JMR.SDD.ForoHub.respuestas.entity.Respuesta;
import org.springframework.stereotype.Component;

/**
 * Traduce entre los DTOs del modulo y la entidad JPA.
 */
@Component
public class RespuestaMapper {

    /**
     * Convierte el request en una entidad nueva, sin id, topico, estado ni fecha.
     * Uso exclusivo en la creacion: el service completa el resto (R1).
     */
    public Respuesta toDomain(RespuestaDTORequest request) {
        Respuesta respuesta = new Respuesta();
        respuesta.setMensaje(normalizar(request.mensaje()));
        respuesta.setAutor(normalizar(request.autor()));

        return respuesta;
    }

    /**
     * Convierte la entidad en el DTO de salida.
     */
    public RespuestaDTOResponse toResponse(Respuesta respuesta) {
        return new RespuestaDTOResponse(
                respuesta.getId(),
                respuesta.getMensaje(),
                respuesta.getFechaCreacion(),
                respuesta.getEstado(),
                respuesta.getAutor(),
                respuesta.getTopico() == null ? null : respuesta.getTopico().getId()
        );
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim();
    }
}
