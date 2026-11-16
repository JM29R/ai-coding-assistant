package JMR.SDD.ForoHub.Topicos.service;

import JMR.SDD.ForoHub.Topicos.controller.TopicoDTORequest;
import JMR.SDD.ForoHub.Topicos.controller.TopicoDTOResponse;
import JMR.SDD.ForoHub.Topicos.entity.EstadoTopico;
import JMR.SDD.ForoHub.Topicos.entity.Topico;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Traduce entre los DTOs del modulo y la entidad JPA.
 */
@Component
public class TopicoMapper {

    /**
     * Convierte el request en una entidad nueva, sin id ni estado ni fecha.
     * Uso exclusivo en la creacion.
     */
    public Topico toDomain(TopicoDTORequest request) {
        Topico topico = new Topico();
        topico.setTitulo(normalizar(request.titulo()));
        topico.setMensaje(normalizar(request.mensaje()));
        topico.setFechaCreacion(LocalDateTime.now());
        topico.setEstado(EstadoTopico.ACTIVO);
        topico.setAutor(normalizar(request.autor()));

        return topico;
    }

    /**
     * Convierte la entidad en el DTO de salida.
     */
    public TopicoDTOResponse toResponse(Topico topico) {
        return new TopicoDTOResponse(
                topico.getId(),
                topico.getTitulo(),
                topico.getMensaje(),
                topico.getFechaCreacion(),
                topico.getEstado(),
                topico.getAutor()
        );
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim();
    }
}
