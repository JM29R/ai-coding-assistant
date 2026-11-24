package JMR.SDD.TicketMaster.evento.exception;

public class EventoNotFoundException extends RuntimeException {

    public EventoNotFoundException(String mensaje) {
        super(mensaje);
    }
}
