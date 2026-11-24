package JMR.SDD.TicketMaster.evento.controller.DTOs;

import java.time.LocalDateTime;

public record EventoDTOResponse(
        Long id,
        String titulo,
        String descripcion,
        LocalDateTime inicio,
        LocalDateTime fin,
        String ubicacion,
        Integer aforo,
        String registrador,
        String estado
) {
}
