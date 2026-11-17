package JMR.SDD.ForoHub.respuestas.controller;

import JMR.SDD.ForoHub.respuestas.service.RespuestasService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Endpoints REST del modulo de respuestas. Sin logica de negocio.
 */
@RestController
@RequestMapping("/respuestas")
public class RespuestasController {

    private final RespuestasService respuestasService;

    public RespuestasController(RespuestasService respuestasService) {
        this.respuestasService = respuestasService;
    }

    @PostMapping
    public ResponseEntity<RespuestaDTOResponse> crear(@RequestBody RespuestaDTORequest request) {
        RespuestaDTOResponse creada = respuestasService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creada.id())
                .toUri();
        return ResponseEntity.created(location).body(creada);
    }

    @GetMapping("/topico/{topicoId}")
    public ResponseEntity<List<RespuestaDTOResponse>> listarPorTopico(@PathVariable Long topicoId) {
        return ResponseEntity.ok(respuestasService.listarPorTopico(topicoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaDTOResponse> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(respuestasService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RespuestaDTOResponse> actualizar(
            @PathVariable Long id,
            @RequestBody(required = false) RespuestaDTORequest request) {
        return ResponseEntity.ok(respuestasService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        respuestasService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
