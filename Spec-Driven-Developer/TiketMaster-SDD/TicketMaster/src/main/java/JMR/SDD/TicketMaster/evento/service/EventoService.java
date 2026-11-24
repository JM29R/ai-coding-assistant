package JMR.SDD.TicketMaster.evento.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import JMR.SDD.TicketMaster.evento.controller.DTOs.EventoDTORequest;
import JMR.SDD.TicketMaster.evento.controller.DTOs.EventoDTOResponse;
import JMR.SDD.TicketMaster.evento.controller.DTOs.MensajeEventoResponse;
import org.springframework.stereotype.Service;

import JMR.SDD.TicketMaster.evento.entity.Evento;
import JMR.SDD.TicketMaster.evento.entity.EventoEstado;
import JMR.SDD.TicketMaster.evento.entity.EventoRepository;
import JMR.SDD.TicketMaster.evento.exception.EventoBusinessException;
import JMR.SDD.TicketMaster.evento.exception.EventoNotFoundException;

@Service
public class EventoService {

    private static final String MENSAJE_BAJA = "Evento dado de baja";
    private static final String MENSAJE_YA_BAJA = "El evento ya estaba dado de baja";

    private final EventoRepository eventoRepository;
    private final EventoDTOMapper eventoDTOMapper;

    public EventoService(EventoRepository eventoRepository, EventoDTOMapper eventoDTOMapper) {
        this.eventoRepository = eventoRepository;
        this.eventoDTOMapper = eventoDTOMapper;
    }

    public EventoDTOResponse crear(EventoDTORequest request) {
        LocalDateTime ahora = LocalDateTime.now();
        if (!request.fin().isAfter(request.inicio())) {
            throw new EventoBusinessException("La fecha de fin debe ser posterior a la fecha de inicio");
        }
        if (request.inicio().isBefore(ahora)) {
            throw new EventoBusinessException("La fecha de inicio no puede estar en el pasado");
        }
        Evento guardado = eventoRepository.save(eventoDTOMapper.toDomain(request));
        return eventoDTOMapper.toResponse(guardado, ahora);
    }

    public EventoDTOResponse publicar(Long id) {
        LocalDateTime ahora = LocalDateTime.now();
        Evento evento = buscarEntidad(id);
        if (evento.getEstado() != EventoEstado.BORRADOR) {
            throw new EventoBusinessException("Solo se puede publicar un evento en estado Borrador");
        }
        if (evento.getInicio().isBefore(ahora)) {
            throw new EventoBusinessException("No se puede publicar un evento cuyo inicio ya pasó");
        }
        evento.setEstado(EventoEstado.PUBLICADO);
        Evento guardado = eventoRepository.save(evento);
        return eventoDTOMapper.toResponse(guardado, ahora);
    }

    public MensajeEventoResponse darDeBaja(Long id) {
        LocalDateTime ahora = LocalDateTime.now();
        Evento evento = buscarEntidad(id);
        if (evento.getEstado() == EventoEstado.ELIMINADO) {
            return new MensajeEventoResponse(MENSAJE_YA_BAJA, eventoDTOMapper.toResponse(evento, ahora));
        }
        evento.setEstado(EventoEstado.ELIMINADO);
        Evento guardado = eventoRepository.save(evento);
        return new MensajeEventoResponse(MENSAJE_BAJA, eventoDTOMapper.toResponse(guardado, ahora));
    }

    public List<EventoDTOResponse> listar(String estado, LocalDateTime desde, LocalDateTime hasta, String titulo) {
        LocalDateTime ahora = LocalDateTime.now();
        FiltroEstado filtro = normalizarEstado(estado);
        String tituloNormalizado = normalizarTitulo(titulo);
        return eventoRepository
                .listarConFiltros(filtro.efectivo(), filtro.declarado(), desde, hasta, tituloNormalizado, ahora)
                .stream()
                .map(evento -> eventoDTOMapper.toResponse(evento, ahora))
                .toList();
    }

    public List<EventoDTOResponse> listarTodos() {
        return listar(null, null, null, null);
    }

    public EventoDTOResponse buscarPorId(Long id) {
        LocalDateTime ahora = LocalDateTime.now();
        return eventoDTOMapper.toResponse(buscarEntidad(id), ahora);
    }

    public List<EventoDTOResponse> listarEliminados() {
        LocalDateTime ahora = LocalDateTime.now();
        return eventoRepository
                .listarConFiltros(null, EventoEstado.ELIMINADO, null, null, null, ahora)
                .stream()
                .map(evento -> eventoDTOMapper.toResponse(evento, ahora))
                .toList();
    }

    private Evento buscarEntidad(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new EventoNotFoundException("Evento no encontrado con id " + id));
    }

    private String normalizarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            return null;
        }
        return titulo.trim();
    }

    private FiltroEstado normalizarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return new FiltroEstado(null, null);
        }
        String clave = estado.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s_-]+", "");
        return switch (clave) {
            case "BORRADOR" -> new FiltroEstado(null, EventoEstado.BORRADOR);
            case "CANCELADO" -> new FiltroEstado(null, EventoEstado.CANCELADO);
            case "ELIMINADO" -> new FiltroEstado(null, EventoEstado.ELIMINADO);
            case "PUBLICADO" -> new FiltroEstado("PUBLICADO", null);
            case "ENCURSO" -> new FiltroEstado("EN_CURSO", null);
            case "FINALIZADO" -> new FiltroEstado("FINALIZADO", null);
            default -> throw new EventoBusinessException(
                    "Estado inválido: '" + estado
                            + "'. Valores permitidos: Borrador, Publicado, En curso, Finalizado, Cancelado, Eliminado");
        };
    }

    private record FiltroEstado(String efectivo, EventoEstado declarado) {
    }
}
