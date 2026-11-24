package JMR.SDD.TicketMaster.evento.service;

import java.time.LocalDateTime;

import JMR.SDD.TicketMaster.evento.controller.DTOs.EventoDTORequest;
import JMR.SDD.TicketMaster.evento.controller.DTOs.EventoDTOResponse;
import org.springframework.stereotype.Component;

import JMR.SDD.TicketMaster.evento.entity.Evento;
import JMR.SDD.TicketMaster.evento.entity.EventoEstado;

@Component
public class EventoDTOMapper {

    public Evento toDomain(EventoDTORequest request) {
        Evento evento = new Evento();
        evento.setTitulo(request.titulo());
        evento.setDescripcion(request.descripcion());
        evento.setInicio(request.inicio());
        evento.setFin(request.fin());
        evento.setUbicacion(request.ubicacion());
        evento.setAforo(request.aforo());
        evento.setRegistrador(request.registrador());
        evento.setEstado(EventoEstado.BORRADOR);
        return evento;
    }

    public EventoDTOResponse toResponse(Evento evento, LocalDateTime ahora) {
        return new EventoDTOResponse(
                evento.getId(),
                evento.getTitulo(),
                evento.getDescripcion(),
                evento.getInicio(),
                evento.getFin(),
                evento.getUbicacion(),
                evento.getAforo(),
                evento.getRegistrador(),
                estadoEfectivo(evento, ahora));
    }

    private String estadoEfectivo(Evento evento, LocalDateTime ahora) {
        if (evento.getEstado() != EventoEstado.PUBLICADO) {
            return switch (evento.getEstado()) {
                case BORRADOR -> "Borrador";
                case CANCELADO -> "Cancelado";
                case ELIMINADO -> "Eliminado";
                case PUBLICADO -> "Publicado";
            };
        }
        if (ahora.isBefore(evento.getInicio())) {
            return "Publicado";
        }
        if (ahora.isAfter(evento.getFin())) {
            return "Finalizado";
        }
        return "En curso";
    }
}
