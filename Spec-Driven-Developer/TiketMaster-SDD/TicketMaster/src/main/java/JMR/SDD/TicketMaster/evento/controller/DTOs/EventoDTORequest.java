package JMR.SDD.TicketMaster.evento.controller.DTOs;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EventoDTORequest(
        @NotBlank(message = "El título es obligatorio")
        String titulo,

        @NotBlank(message = "La descripción es obligatoria")
        String descripcion,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDateTime inicio,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDateTime fin,

        @NotBlank(message = "La ubicación es obligatoria")
        String ubicacion,

        @NotNull(message = "El aforo es obligatorio")
        @Min(value = 1, message = "El aforo debe ser al menos 1")
        Integer aforo,

        @NotBlank(message = "El registrador es obligatorio")
        String registrador
) {
}
