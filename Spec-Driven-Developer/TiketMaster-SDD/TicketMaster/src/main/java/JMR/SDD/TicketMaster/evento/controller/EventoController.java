package JMR.SDD.TicketMaster.evento.controller;

import java.time.LocalDateTime;
import java.util.List;

import JMR.SDD.TicketMaster.evento.controller.DTOs.EventoDTORequest;
import JMR.SDD.TicketMaster.evento.controller.DTOs.EventoDTOResponse;
import JMR.SDD.TicketMaster.evento.controller.DTOs.MensajeEventoResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import JMR.SDD.TicketMaster.evento.service.EventoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/eventos")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @PostMapping
    public ResponseEntity<EventoDTOResponse> crear(@Valid @RequestBody EventoDTORequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.crear(request));
    }

    @PostMapping("/publicar/{id}")
    public ResponseEntity<EventoDTOResponse> publicar(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.publicar(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MensajeEventoResponse> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.darDeBaja(id));
    }

    @GetMapping
    public ResponseEntity<List<EventoDTOResponse>> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) String titulo) {
        return ResponseEntity.ok(eventoService.listar(estado, desde, hasta, titulo));
    }

    @GetMapping("/todos")
    public ResponseEntity<List<EventoDTOResponse>> listarTodos() {
        return ResponseEntity.ok(eventoService.listarTodos());
    }

    @GetMapping("/eliminados")
    public ResponseEntity<List<EventoDTOResponse>> listarEliminados() {
        return ResponseEntity.ok(eventoService.listarEliminados());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoDTOResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.buscarPorId(id));
    }
}
