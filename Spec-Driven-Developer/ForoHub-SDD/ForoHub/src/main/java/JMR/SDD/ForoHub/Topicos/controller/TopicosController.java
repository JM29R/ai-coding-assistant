package JMR.SDD.ForoHub.Topicos.controller;

import JMR.SDD.ForoHub.Topicos.service.TopicosService;
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
 * Endpoints REST del modulo de topicos. Sin logica de negocio.
 */
@RestController
@RequestMapping("/topicos")
public class TopicosController {

    private final TopicosService topicosService;

    public TopicosController(TopicosService topicosService) {
        this.topicosService = topicosService;
    }

    @PostMapping("/create")
    public ResponseEntity<TopicoDTOResponse> crear(@RequestBody TopicoDTORequest request) {
        TopicoDTOResponse creado = topicosService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @GetMapping("/list")
    public ResponseEntity<List<TopicoDTOResponse>> listar() {
        return ResponseEntity.ok(topicosService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TopicoDTOResponse> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(topicosService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TopicoDTOResponse> actualizar(
            @PathVariable Long id,
            @RequestBody TopicoDTORequest request) {
        return ResponseEntity.ok(topicosService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        topicosService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
