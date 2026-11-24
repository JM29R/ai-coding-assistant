package JMR.SDD.TicketMaster.evento.entity;

/**
 * Estados declarados de un evento.
 *
 * <p>Los estados {@code EN_CURSO} y {@code FINALIZADO} no se persisten: se calculan
 * comparando la fecha/hora actual con las fechas de inicio y fin.</p>
 */
public enum EventoEstado {
    BORRADOR,
    PUBLICADO,
    CANCELADO,
    ELIMINADO
}
